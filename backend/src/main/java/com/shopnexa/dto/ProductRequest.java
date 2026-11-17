package com.shopnexa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 180) String name,
        @Size(max = 200) String slug,
        @NotBlank @Size(max = 80) String sku,
        @Size(max = 10000) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal discountPrice,
        @PositiveOrZero Integer stock,
        Boolean active,
        @Size(max = 10) List<@NotBlank @Size(max = 1000) String> imageUrls
) {
}
