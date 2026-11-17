package com.shopnexa.controller;

import com.shopnexa.dto.CheckoutRequest;
import com.shopnexa.dto.CheckoutResponse;
import com.shopnexa.dto.OrderResponse;
import com.shopnexa.dto.PaymentOptionsResponse;
import com.shopnexa.dto.PaymentVerificationRequest;
import com.shopnexa.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/checkout/options")
    public PaymentOptionsResponse getPaymentOptions() {
        return orderService.getPaymentOptions();
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResponse checkout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return orderService.checkout(authentication.getName(), request);
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrder(Authentication authentication, @PathVariable Long orderId) {
        return orderService.getOrder(authentication.getName(), orderId);
    }

    @PostMapping("/orders/{orderId}/verify-payment")
    public OrderResponse verifyPayment(
            Authentication authentication,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentVerificationRequest request
    ) {
        return orderService.verifyPayment(authentication.getName(), orderId, request);
    }

    @PostMapping("/orders/{orderId}/cancel-payment")
    public OrderResponse cancelPayment(Authentication authentication, @PathVariable Long orderId) {
        return orderService.cancelPendingPayment(authentication.getName(), orderId);
    }
}
