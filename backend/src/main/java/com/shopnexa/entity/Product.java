package com.shopnexa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 12, scale = 2)
    private BigDecimal discountPrice;

    @Column(nullable = false)
    private int stock;

    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {
    }

    public Product(
            Category category,
            String name,
            String slug,
            String sku,
            String description,
            BigDecimal price,
            BigDecimal discountPrice,
            int stock,
            boolean active
    ) {
        this.category = category;
        update(name, slug, sku, description, price, discountPrice, stock, active);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getSku() {
        return sku;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public int getStock() {
        return stock;
    }

    public void decreaseStock(int quantity) {
        if (quantity < 1 || quantity > stock) {
            throw new IllegalArgumentException("Stock reduction must be positive and available.");
        }
        stock -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Stock increase must be positive.");
        }
        stock += quantity;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void update(
            String name,
            String slug,
            String sku,
            String description,
            BigDecimal price,
            BigDecimal discountPrice,
            int stock,
            boolean active
    ) {
        this.name = name;
        this.slug = slug;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.discountPrice = discountPrice;
        this.stock = stock;
        this.active = active;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void replaceImages(List<String> imageUrls) {
        images.clear();
        for (int index = 0; index < imageUrls.size(); index++) {
            ProductImage image = new ProductImage(imageUrls.get(index), index);
            image.setProduct(this);
            images.add(image);
        }
    }

    public void deactivate() {
        active = false;
    }
}
