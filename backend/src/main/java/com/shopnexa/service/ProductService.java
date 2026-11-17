package com.shopnexa.service;

import com.shopnexa.dto.ProductRequest;
import com.shopnexa.dto.ProductResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface ProductService {
    Page<ProductResponse> getProducts(
            String query,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            int page,
            int size,
            String sort
    );

    ProductResponse getProduct(Long id);

    Page<ProductResponse> getAllAdminProducts(Pageable pageable);

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);
}
