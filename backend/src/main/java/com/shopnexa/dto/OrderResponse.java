package com.shopnexa.dto;

import com.shopnexa.enums.OrderStatus;
import com.shopnexa.enums.PaymentMethod;
import com.shopnexa.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal subtotal,
        BigDecimal deliveryCharge,
        BigDecimal total,
        String recipientName,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        Instant createdAt,
        List<OrderItemResponse> items
) {
}
