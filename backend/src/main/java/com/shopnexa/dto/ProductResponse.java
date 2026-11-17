package com.shopnexa.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String slug,
        String sku,
        String description,
        BigDecimal price,
        BigDecimal discountPrice,
        int stock,
        BigDecimal ratingAverage,
        int ratingCount,
        boolean active,
        List<String> imageUrls,
        Instant createdAt
) {
}
