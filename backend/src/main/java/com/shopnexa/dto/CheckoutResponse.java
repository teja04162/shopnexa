package com.shopnexa.dto;

import com.shopnexa.enums.OrderStatus;
import com.shopnexa.enums.PaymentMethod;

import java.math.BigDecimal;

public record CheckoutResponse(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal deliveryCharge,
        BigDecimal total,
        boolean paymentRequired,
        String razorpayKeyId,
        String razorpayOrderId,
        Long amountInPaise,
        String currency
) {
}
