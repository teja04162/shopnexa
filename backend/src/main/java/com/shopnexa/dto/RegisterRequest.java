package com.shopnexa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 30) String phone,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 72) String confirmPassword,
        @NotBlank @Size(max = 200) String addressLine1,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String state,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 100) String country
) {
}
