package com.shopnexa.service.impl;

import com.shopnexa.dto.AddCartItemRequest;
import com.shopnexa.dto.CartItemResponse;
import com.shopnexa.dto.CartResponse;
import com.shopnexa.dto.UpdateCartItemRequest;
import com.shopnexa.entity.Cart;
import com.shopnexa.entity.CartItem;
import com.shopnexa.entity.Product;
import com.shopnexa.entity.User;
import com.shopnexa.exception.BadRequestException;
import com.shopnexa.exception.ResourceNotFoundException;
import com.shopnexa.repository.CartRepository;
import com.shopnexa.repository.ProductRepository;
import com.shopnexa.repository.UserRepository;
import com.shopnexa.service.CartService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BigDecimal deliveryCharge;
    private final BigDecimal freeDeliveryThreshold;

    public CartServiceImpl(
            CartRepository cartRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            @Value("${app.delivery.flat-charge:50.00}") BigDecimal deliveryCharge,
            @Value("${app.delivery.free-threshold:1000.00}") BigDecimal freeDeliveryThreshold
    ) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.deliveryCharge = deliveryCharge;
        this.freeDeliveryThreshold = freeDeliveryThreshold;
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        return cartRepository.findByUserEmailIgnoreCase(email)
                .map(this::toResponse)
                .orElseGet(this::emptyCart);
    }

    @Override
    @Transactional
    public CartResponse addItem(String email, AddCartItemRequest request) {
        Product product = findAvailableProduct(request.productId());
        Cart cart = getOrCreateCart(email);
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);
        int quantity = request.quantity() + (existingItem == null ? 0 : existingItem.getQuantity());
        ensureStock(product, quantity);
        if (existingItem == null) {
            cart.addItem(new CartItem(product, quantity));
        } else {
            existingItem.setQuantity(quantity);
        }
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(String email, Long itemId, UpdateCartItemRequest request) {
        Cart cart = findCart(email);
        CartItem item = findCartItem(cart, itemId);
        Product product = findAvailableProduct(item.getProduct().getId());
        ensureStock(product, request.quantity());
        item.setQuantity(request.quantity());
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(String email, Long itemId) {
        Cart cart = findCart(email);
        cart.removeItem(findCartItem(cart, itemId));
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse clear(String email) {
        return cartRepository.findByUserEmailIgnoreCase(email)
                .map(cart -> {
                    cart.clear();
                    return toResponse(cart);
                })
                .orElseGet(this::emptyCart);
    }

    private Cart getOrCreateCart(String email) {
        return cartRepository.findByUserEmailIgnoreCase(email).orElseGet(() -> {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User account was not found."));
            return cartRepository.save(new Cart(user));
        });
    }

    private Cart findCart(String email) {
        return cartRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping cart was not found."));
    }

    private CartItem findCartItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item was not found."));
    }

    private Product findAvailableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product was not found."));
        if (!product.isActive() || !product.getCategory().isActive()) {
            throw new ResourceNotFoundException("Product is not available.");
        }
        return product;
    }

    private void ensureStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new BadRequestException("Requested quantity exceeds available stock.");
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> {
                    Product product = item.getProduct();
                    BigDecimal price = product.getDiscountPrice() == null
                            ? product.getPrice()
                            : product.getDiscountPrice();
                    BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new CartItemResponse(
                            item.getId(),
                            product.getId(),
                            product.getName(),
                            product.getCategory().getName(),
                            price,
                            item.getQuantity(),
                            product.getStock(),
                            product.getImages().stream().map(image -> image.getImageUrl()).toList(),
                            lineTotal
                    );
                })
                .toList();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = items.isEmpty() || subtotal.compareTo(freeDeliveryThreshold) >= 0
                ? BigDecimal.ZERO
                : deliveryCharge;
        int itemCount = items.stream().mapToInt(CartItemResponse::quantity).sum();
        return new CartResponse(items, itemCount, subtotal, shipping, subtotal.add(shipping));
    }

    private CartResponse emptyCart() {
        return new CartResponse(List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
