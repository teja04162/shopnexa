package com.shopnexa.service;

import com.shopnexa.dto.WishlistResponse;

public interface WishlistService {
    WishlistResponse getWishlist(String email);

    WishlistResponse addProduct(String email, Long productId);

    WishlistResponse removeProduct(String email, Long productId);
}
