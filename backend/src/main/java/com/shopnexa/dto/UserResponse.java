package com.shopnexa.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        List<String> roles
) {
}
