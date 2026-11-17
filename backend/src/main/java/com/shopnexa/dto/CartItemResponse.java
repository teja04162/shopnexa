package com.shopnexa.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        String categoryName,
        BigDecimal unitPrice,
        int quantity,
        int availableStock,
        List<String> imageUrls,
        BigDecimal lineTotal
) {
}
