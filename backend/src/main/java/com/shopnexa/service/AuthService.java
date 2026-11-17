package com.shopnexa.service;

import com.shopnexa.dto.AuthResponse;
import com.shopnexa.dto.LoginRequest;
import com.shopnexa.dto.RegisterRequest;
import com.shopnexa.dto.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    void logout(String email, String token);

    UserResponse currentUser(String email);
}
