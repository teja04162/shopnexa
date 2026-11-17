package com.shopnexa.controller;

import com.shopnexa.dto.AuthResponse;
import com.shopnexa.dto.LoginRequest;
import com.shopnexa.dto.RegisterRequest;
import com.shopnexa.dto.UserResponse;
import com.shopnexa.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Authentication authentication,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        authService.logout(authentication.getName(), authorizationHeader);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return authService.currentUser(authentication.getName());
    }
}
