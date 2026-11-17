package com.shopnexa.service.impl;

import com.shopnexa.dto.CheckoutRequest;
import com.shopnexa.dto.CheckoutResponse;
import com.shopnexa.dto.OrderItemResponse;
import com.shopnexa.dto.OrderResponse;
import com.shopnexa.dto.PaymentOptionsResponse;
import com.shopnexa.dto.PaymentVerificationRequest;
import com.shopnexa.entity.Cart;
import com.shopnexa.entity.CartItem;
import com.shopnexa.entity.Order;
import com.shopnexa.entity.OrderItem;
import com.shopnexa.entity.Payment;
import com.shopnexa.entity.Product;
import com.shopnexa.entity.User;
import com.shopnexa.enums.OrderStatus;
import com.shopnexa.enums.PaymentMethod;
import com.shopnexa.enums.PaymentProvider;
import com.shopnexa.enums.PaymentStatus;
import com.shopnexa.exception.BadRequestException;
import com.shopnexa.exception.ResourceNotFoundException;
import com.shopnexa.repository.CartRepository;
import com.shopnexa.repository.OrderRepository;
import com.shopnexa.repository.ProductRepository;
import com.shopnexa.repository.UserRepository;
import com.shopnexa.service.OrderService;
import com.shopnexa.service.RazorpayService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RazorpayService razorpayService;
    private final BigDecimal deliveryCharge;
    private final BigDecimal freeDeliveryThreshold;

    public OrderServiceImpl(
            CartRepository cartRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            RazorpayService razorpayService,
            @Value("${app.delivery.flat-charge:50.00}") BigDecimal deliveryCharge,
            @Value("${app.delivery.free-threshold:1000.00}") BigDecimal freeDeliveryThreshold
    ) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.razorpayService = razorpayService;
        this.deliveryCharge = deliveryCharge;
        this.freeDeliveryThreshold = freeDeliveryThreshold;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentOptionsResponse getPaymentOptions() {
        return new PaymentOptionsResponse(true, razorpayService.isEnabled(), razorpayService.getKeyId());
    }

    @Override
    @Transactional
    public CheckoutResponse checkout(String email, CheckoutRequest request) {
        if (request.paymentMethod() == PaymentMethod.RAZORPAY && !razorpayService.isEnabled()) {
            throw new BadRequestException("Online payment is not configured. Choose cash on delivery.");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found."));
        Cart cart = cartRepository.findByUserEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new BadRequestException("Your shopping cart is empty."));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your shopping cart is empty.");
        }

        List<CartItem> cartItems = new ArrayList<>(cart.getItems());
        List<Long> productIds = cartItems.stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .sorted()
                .toList();
        Map<Long, Product> lockedProducts = new HashMap<>();
        for (Long productId : productIds) {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("A product in your cart is no longer available."));
            if (!product.isActive() || !product.getCategory().isActive()) {
                throw new BadRequestException(product.getName() + " is no longer available.");
            }
            lockedProducts.put(productId, product);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            if (cartItem.getQuantity() > product.getStock()) {
                throw new BadRequestException(
                        "Only " + product.getStock() + " of " + product.getName() + " are currently available."
                );
            }
            BigDecimal quantity = BigDecimal.valueOf(cartItem.getQuantity());
            BigDecimal price = unitPrice(product);
            subtotal = subtotal.add(price.multiply(quantity));
            discountTotal = discountTotal.add(product.getPrice().subtract(price).multiply(quantity));
        }

        BigDecimal shipping = subtotal.compareTo(freeDeliveryThreshold) >= 0
                ? BigDecimal.ZERO
                : deliveryCharge;
        BigDecimal total = subtotal.add(shipping);
        PaymentMethod method = request.paymentMethod();
        Order order = new Order(
                user,
                createOrderNumber(),
                method == PaymentMethod.COD ? OrderStatus.PLACED : OrderStatus.PENDING_PAYMENT,
                method,
                subtotal,
                discountTotal,
                shipping,
                total,
                request.recipientName().trim(),
                request.phone().trim(),
                request.addressLine1().trim(),
                cleanOptional(request.addressLine2()),
                request.city().trim(),
                request.state().trim(),
                request.postalCode().trim(),
                request.country().trim()
        );

        for (CartItem cartItem : cartItems) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            int quantity = cartItem.getQuantity();
            product.decreaseStock(quantity);
            order.addItem(new OrderItem(
                    product,
                    product.getName(),
                    product.getSku(),
                    unitPrice(product),
                    quantity
            ));
        }

        Payment payment = new Payment(
                method == PaymentMethod.COD ? PaymentProvider.COD : PaymentProvider.RAZORPAY,
                PaymentStatus.PENDING,
                total
        );
        order.setPayment(payment);

        RazorpayService.GatewayOrder gatewayOrder = null;
        if (method == PaymentMethod.RAZORPAY) {
            gatewayOrder = razorpayService.createOrder(total, order.getOrderNumber());
            payment.setProviderOrderId(gatewayOrder.id());
        }

        orderRepository.save(order);
        cart.clear();

        return new CheckoutResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                method,
                subtotal,
                shipping,
                total,
                method == PaymentMethod.RAZORPAY,
                gatewayOrder == null ? null : razorpayService.getKeyId(),
                gatewayOrder == null ? null : gatewayOrder.id(),
                gatewayOrder == null ? null : gatewayOrder.amountInPaise(),
                gatewayOrder == null ? null : gatewayOrder.currency()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String email, Long orderId) {
        Order order = orderRepository.findByIdAndUserEmailIgnoreCase(orderId, email)
                .orElseThrow(() -> new ResourceNotFoundException("Order was not found."));
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse verifyPayment(String email, Long orderId, PaymentVerificationRequest request) {
        Order order = findOrderForUpdate(orderId);
        ensureOwner(order, email);
        Payment payment = order.getPayment();
        if (payment == null || payment.getProvider() != PaymentProvider.RAZORPAY
                || !request.razorpayOrderId().equals(payment.getProviderOrderId())) {
            throw new BadRequestException("Payment does not match this order.");
        }
        razorpayService.verifySignature(
                payment.getProviderOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature()
        );

        if (order.getStatus() == OrderStatus.PLACED && payment.getStatus() == PaymentStatus.PAID
                && request.razorpayPaymentId().equals(payment.getProviderPaymentId())) {
            return toResponse(order);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("This payment is no longer pending.");
        }

        payment.markPaid(request.razorpayPaymentId());
        order.markPlaced();
        return toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelPendingPayment(String email, Long orderId) {
        Order order = findOrderForUpdate(orderId);
        ensureOwner(order, email);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toResponse(order);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Only a pending payment can be cancelled.");
        }
        releaseReservation(order);
        return toResponse(order);
    }

    @Override
    @Transactional
    public void expirePendingPayment(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }
        releaseReservation(order);
    }

    private void releaseReservation(Order order) {
        Payment payment = order.getPayment();
        if (payment != null) {
            payment.markFailed();
        }
        order.cancel();

        String email = order.getUser().getEmail();
        Cart cart = cartRepository.findByUserEmailIgnoreCaseForUpdate(email).orElseGet(() -> {
            Cart newCart = new Cart(order.getUser());
            return cartRepository.save(newCart);
        });
        for (OrderItem orderItem : order.getItems()) {
            Product product = orderItem.getProduct();
            if (product == null) {
                continue;
            }
            Product lockedProduct = productRepository.findByIdForUpdate(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("A reserved product could not be restored."));
            lockedProduct.increaseStock(orderItem.getQuantity());

            CartItem existingItem = cart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(lockedProduct.getId()))
                    .findFirst()
                    .orElse(null);
            if (existingItem == null) {
                cart.addItem(new CartItem(lockedProduct, orderItem.getQuantity()));
            } else {
                int restoredQuantity = existingItem.getQuantity() + orderItem.getQuantity();
                existingItem.setQuantity(Math.min(restoredQuantity, lockedProduct.getStock()));
            }
        }
    }

    private Order findOrderForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order was not found."));
    }

    private void ensureOwner(Order order, String email) {
        if (!order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ResourceNotFoundException("Order was not found.");
        }
    }

    private BigDecimal unitPrice(Product product) {
        return product.getDiscountPrice() == null ? product.getPrice() : product.getDiscountPrice();
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String createOrderNumber() {
        return "SNX-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct() == null ? null : item.getProduct().getId(),
                        item.getProductName(),
                        item.getProductSku(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()
                ))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPayment() == null ? PaymentStatus.PENDING : order.getPayment().getStatus(),
                order.getSubtotal(),
                order.getDeliveryCharge(),
                order.getTotal(),
                order.getRecipientName(),
                order.getPhone(),
                order.getAddressLine1(),
                order.getAddressLine2(),
                order.getCity(),
                order.getState(),
                order.getPostalCode(),
                order.getCountry(),
                order.getCreatedAt(),
                items
        );
    }
}
