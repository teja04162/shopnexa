package com.shopnexa.service.impl;

import com.shopnexa.dto.CheckoutRequest;
import com.shopnexa.dto.CheckoutResponse;
import com.shopnexa.entity.Cart;
import com.shopnexa.entity.CartItem;
import com.shopnexa.entity.Category;
import com.shopnexa.entity.Order;
import com.shopnexa.entity.Product;
import com.shopnexa.entity.User;
import com.shopnexa.enums.PaymentMethod;
import com.shopnexa.exception.BadRequestException;
import com.shopnexa.repository.CartRepository;
import com.shopnexa.repository.OrderRepository;
import com.shopnexa.repository.ProductRepository;
import com.shopnexa.repository.UserRepository;
import com.shopnexa.service.RazorpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RazorpayService razorpayService;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                cartRepository,
                orderRepository,
                productRepository,
                userRepository,
                razorpayService,
                new BigDecimal("50.00"),
                new BigDecimal("1000.00")
        );
    }

    @Test
    void codCheckoutUsesServerPriceAndClearsCart() {
        User user = new User("Shopper", "shopper@example.com", "555-0123", "hash");
        Category category = org.mockito.Mockito.mock(Category.class);
        Product product = org.mockito.Mockito.mock(Product.class);
        Cart cart = new Cart(user);
        cart.addItem(new CartItem(product, 2));
        when(userRepository.findByEmailIgnoreCase("shopper@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserEmailIgnoreCaseForUpdate("shopper@example.com")).thenReturn(Optional.of(cart));
        when(product.getId()).thenReturn(7L);
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));
        when(product.isActive()).thenReturn(true);
        when(product.getCategory()).thenReturn(category);
        when(category.isActive()).thenReturn(true);
        when(product.getStock()).thenReturn(4);
        when(product.getDiscountPrice()).thenReturn(null);
        when(product.getPrice()).thenReturn(new BigDecimal("100.00"));
        when(product.getName()).thenReturn("Desk Lamp");
        when(product.getSku()).thenReturn("LAMP-7");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutResponse response = orderService.checkout("shopper@example.com", validRequest());

        assertEquals(new BigDecimal("200.00"), response.subtotal());
        assertEquals(new BigDecimal("50.00"), response.deliveryCharge());
        assertEquals(new BigDecimal("250.00"), response.total());
        assertEquals(PaymentMethod.COD, response.paymentMethod());
        assertEquals(0, cart.getItems().size());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(1, orderCaptor.getValue().getItems().size());
        verify(product).decreaseStock(2);
    }

    @Test
    void checkoutRejectsInsufficientStockWithoutCreatingOrder() {
        User user = new User("Shopper", "shopper@example.com", "555-0123", "hash");
        Category category = org.mockito.Mockito.mock(Category.class);
        Product product = org.mockito.Mockito.mock(Product.class);
        Cart cart = new Cart(user);
        cart.addItem(new CartItem(product, 3));
        when(userRepository.findByEmailIgnoreCase("shopper@example.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserEmailIgnoreCaseForUpdate("shopper@example.com")).thenReturn(Optional.of(cart));
        when(product.getId()).thenReturn(7L);
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));
        when(product.isActive()).thenReturn(true);
        when(product.getCategory()).thenReturn(category);
        when(category.isActive()).thenReturn(true);
        when(product.getStock()).thenReturn(2);
        when(product.getName()).thenReturn("Desk Lamp");

        assertThrows(BadRequestException.class, () -> orderService.checkout("shopper@example.com", validRequest()));

        verify(orderRepository, never()).save(any(Order.class));
        verify(product, never()).decreaseStock(org.mockito.ArgumentMatchers.anyInt());
        assertEquals(1, cart.getItems().size());
    }

    private CheckoutRequest validRequest() {
        return new CheckoutRequest(
                "Shopper",
                "555-0123",
                "1 Main Street",
                null,
                "Springfield",
                "State",
                "12345",
                "India",
                PaymentMethod.COD
        );
    }
}
