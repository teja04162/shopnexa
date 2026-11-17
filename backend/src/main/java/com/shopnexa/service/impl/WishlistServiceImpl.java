package com.shopnexa.service.impl;

import com.shopnexa.dto.ProductResponse;
import com.shopnexa.dto.WishlistResponse;
import com.shopnexa.entity.Product;
import com.shopnexa.entity.User;
import com.shopnexa.entity.Wishlist;
import com.shopnexa.entity.WishlistItem;
import com.shopnexa.exception.ResourceNotFoundException;
import com.shopnexa.repository.ProductRepository;
import com.shopnexa.repository.UserRepository;
import com.shopnexa.repository.WishlistRepository;
import com.shopnexa.service.WishlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistServiceImpl(
            WishlistRepository wishlistRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(String email) {
        return wishlistRepository.findByUserEmailIgnoreCase(email)
                .map(this::toResponse)
                .orElseGet(() -> new WishlistResponse(List.of()));
    }

    @Override
    @Transactional
    public WishlistResponse addProduct(String email, Long productId) {
        Product product = findAvailableProduct(productId);
        Wishlist wishlist = wishlistRepository.findByUserEmailIgnoreCase(email).orElseGet(() -> {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User account was not found."));
            return wishlistRepository.save(new Wishlist(user));
        });
        boolean alreadySaved = wishlist.getItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(productId));
        if (!alreadySaved) {
            wishlist.addItem(new WishlistItem(product));
        }
        return toResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse removeProduct(String email, Long productId) {
        Wishlist wishlist = wishlistRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist was not found."));
        WishlistItem item = wishlist.getItems().stream()
                .filter(saved -> saved.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item was not found."));
        wishlist.removeItem(item);
        return toResponse(wishlist);
    }

    private Product findAvailableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product was not found."));
        if (!product.isActive() || !product.getCategory().isActive()) {
            throw new ResourceNotFoundException("Product is not available.");
        }
        return product;
    }

    private WishlistResponse toResponse(Wishlist wishlist) {
        return new WishlistResponse(wishlist.getItems().stream()
                .filter(item -> item.getProduct().isActive() && item.getProduct().getCategory().isActive())
                .map(item -> {
                    Product product = item.getProduct();
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
                })
                .toList());
    }
}
