package com.shopnexa.service;

import com.shopnexa.dto.AddCartItemRequest;
import com.shopnexa.dto.CartResponse;
import com.shopnexa.dto.UpdateCartItemRequest;

public interface CartService {
    CartResponse getCart(String email);

    CartResponse addItem(String email, AddCartItemRequest request);

    CartResponse updateItem(String email, Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(String email, Long itemId);

    CartResponse clear(String email);
}
