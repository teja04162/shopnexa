package com.shopnexa.controller;

import com.shopnexa.dto.WishlistResponse;
import com.shopnexa.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public WishlistResponse getWishlist(Authentication authentication) {
        return wishlistService.getWishlist(authentication.getName());
    }

    @PostMapping("/{productId}")
    public WishlistResponse addProduct(Authentication authentication, @PathVariable Long productId) {
        return wishlistService.addProduct(authentication.getName(), productId);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<WishlistResponse> removeProduct(
            Authentication authentication,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(wishlistService.removeProduct(authentication.getName(), productId));
    }
}
