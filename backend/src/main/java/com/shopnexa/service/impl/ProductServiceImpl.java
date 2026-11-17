package com.shopnexa.service.impl;

import com.shopnexa.dto.ProductRequest;
import com.shopnexa.dto.ProductResponse;
import com.shopnexa.entity.Category;
import com.shopnexa.entity.Product;
import com.shopnexa.exception.BadRequestException;
import com.shopnexa.exception.ConflictException;
import com.shopnexa.exception.ResourceNotFoundException;
import com.shopnexa.repository.CategoryRepository;
import com.shopnexa.repository.ProductRepository;
import com.shopnexa.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Map<String, Sort> SORTS = Map.of(
            "newest", Sort.by(Sort.Direction.DESC, "createdAt"),
            "priceAsc", Sort.by(Sort.Direction.ASC, "price"),
            "priceDesc", Sort.by(Sort.Direction.DESC, "price"),
            "rating", Sort.by(Sort.Direction.DESC, "ratingAverage"),
            "nameAsc", Sort.by(Sort.Direction.ASC, "name"),
            "nameDesc", Sort.by(Sort.Direction.DESC, "name")
    );

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(
            String query,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            int page,
            int size,
            String sort
    ) {
        validateFilters(minPrice, maxPrice, minRating, page, size);
        Sort productSort = SORTS.get(sort);
        if (productSort == null) {
            throw new BadRequestException("Unsupported sort option.");
        }

        Specification<Product> specification = activeProducts();
        if (categoryId != null) {
            specification = specification.and((root, queryBuilder, criteria) ->
                    criteria.equal(root.get("category").get("id"), categoryId));
        }
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, queryBuilder, criteria) ->
                    criteria.like(criteria.lower(root.get("name")), pattern));
        }
        if (minPrice != null) {
            specification = specification.and((root, queryBuilder, criteria) ->
                    criteria.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            specification = specification.and((root, queryBuilder, criteria) ->
                    criteria.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        if (minRating != null) {
            specification = specification.and((root, queryBuilder, criteria) ->
                    criteria.greaterThanOrEqualTo(root.get("ratingAverage"), minRating));
        }

        Pageable pageable = PageRequest.of(page, size, productSort);
        return productRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        return productRepository.findOne(activeProducts().and((root, query, criteria) ->
                        criteria.equal(root.get("id"), id)))
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product was not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllAdminProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = findActiveCategory(request.categoryId());
        String slug = normalizeSlug(request.slug(), request.name(), 200);
        String sku = request.sku().trim();
        validateProductFields(request, slug, sku, null);

        Product product = new Product(
                category,
                request.name().trim(),
                slug,
                sku,
                cleanDescription(request.description()),
                request.price(),
                request.discountPrice(),
                request.stock() == null ? 0 : request.stock(),
                request.active() == null || request.active()
        );
        product.replaceImages(cleanImageUrls(request.imageUrls()));
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product was not found."));
        Category category = findActiveCategory(request.categoryId());
        String slug = normalizeSlug(request.slug(), request.name(), 200);
        String sku = request.sku().trim();
        validateProductFields(request, slug, sku, id);

        product.setCategory(category);
        product.update(
                request.name().trim(),
                slug,
                sku,
                cleanDescription(request.description()),
                request.price(),
                request.discountPrice(),
                request.stock() == null ? product.getStock() : request.stock(),
                request.active() == null ? product.isActive() : request.active()
        );
        if (request.imageUrls() != null) {
            product.replaceImages(cleanImageUrls(request.imageUrls()));
        }
        return toResponse(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product was not found."));
        product.deactivate();
    }

    private Specification<Product> activeProducts() {
        return (root, query, criteria) -> criteria.and(
                criteria.isTrue(root.get("active")),
                criteria.isTrue(root.get("category").get("active"))
        );
    }

    private void validateFilters(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page must be non-negative and size must be between 1 and 100.");
        }
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw new BadRequestException("Price filters cannot be negative.");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Minimum price cannot exceed maximum price.");
        }
        if (minRating != null && (minRating.signum() < 0 || minRating.compareTo(BigDecimal.valueOf(5)) > 0)) {
            throw new BadRequestException("Minimum rating must be between 0 and 5.");
        }
    }

    private void validateProductFields(ProductRequest request, String slug, String sku, Long currentId) {
        if (request.discountPrice() != null
                && request.discountPrice().compareTo(request.price()) > 0) {
            throw new BadRequestException("Discount price cannot exceed the regular price.");
        }
        boolean duplicateSlug = currentId == null
                ? productRepository.existsBySlugIgnoreCase(slug)
                : productRepository.existsBySlugIgnoreCaseAndIdNot(slug, currentId);
        if (duplicateSlug) {
            throw new ConflictException("A product with this slug already exists.");
        }
        boolean duplicateSku = currentId == null
                ? productRepository.existsBySkuIgnoreCase(sku)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, currentId);
        if (duplicateSku) {
            throw new ConflictException("A product with this SKU already exists.");
        }
    }

    private Category findActiveCategory(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active category was not found."));
    }

    private List<String> cleanImageUrls(List<String> imageUrls) {
        return imageUrls == null ? List.of() : imageUrls.stream().map(String::trim).toList();
    }

    private String normalizeSlug(String requestedSlug, String name, int maxLength) {
        String source = requestedSlug == null || requestedSlug.isBlank() ? name : requestedSlug;
        String slug = Normalizer.normalize(source.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isEmpty()) {
            throw new BadRequestException("Slug must contain at least one letter or number.");
        }
        return slug.substring(0, Math.min(slug.length(), maxLength)).replaceAll("-+$", "");
    }

    private String cleanDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getStock(),
                product.getRatingAverage(),
                product.getRatingCount(),
                product.isActive(),
                product.getImages().stream().map(image -> image.getImageUrl()).toList(),
                product.getCreatedAt()
        );
    }
}
