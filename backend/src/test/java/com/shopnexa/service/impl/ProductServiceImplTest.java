package com.shopnexa.service.impl;

import com.shopnexa.dto.ProductRequest;
import com.shopnexa.entity.Category;
import com.shopnexa.entity.Product;
import com.shopnexa.exception.BadRequestException;
import com.shopnexa.repository.CategoryRepository;
import com.shopnexa.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void rejectsPriceRangeThatIsInvertedBeforeQueryingRepository() {
        assertThrows(
                BadRequestException.class,
                () -> productService.getProducts(
                        null,
                        null,
                        new BigDecimal("50.00"),
                        new BigDecimal("10.00"),
                        null,
                        0,
                        20,
                        "newest"
                )
        );

        verify(productRepository, never()).findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Product>>any(),
                org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)
        );
    }

    @Test
    void rejectsDiscountAboveRegularPrice() {
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(
                new Category("Books", "books", null, true)
        ));
        ProductRequest request = new ProductRequest(
                1L,
                "A book",
                "a-book",
                "BOOK-1",
                null,
                new BigDecimal("20.00"),
                new BigDecimal("21.00"),
                5,
                true,
                null
        );

        assertThrows(BadRequestException.class, () -> productService.create(request));
        verify(productRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
