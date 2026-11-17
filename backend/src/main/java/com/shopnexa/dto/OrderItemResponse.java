package com.shopnexa.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        String productSku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
