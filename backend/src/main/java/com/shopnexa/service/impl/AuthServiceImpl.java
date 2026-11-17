package com.shopnexa.service.impl;

import com.shopnexa.dto.AuthResponse;
import com.shopnexa.dto.LoginRequest;
import com.shopnexa.dto.RegisterRequest;
import com.shopnexa.dto.UserResponse;
import com.shopnexa.entity.Address;
import com.shopnexa.entity.Role;
import com.shopnexa.entity.User;
import com.shopnexa.exception.BadRequestException;
import com.shopnexa.exception.ConflictException;
import com.shopnexa.exception.ResourceNotFoundException;
import com.shopnexa.exception.UnauthorizedException;
import com.shopnexa.repository.AddressRepository;
import com.shopnexa.repository.RoleRepository;
import com.shopnexa.repository.UserRepository;
import com.shopnexa.security.JwtService;
import com.shopnexa.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AddressRepository addressRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Password and confirmation do not match.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Password must be no more than 72 bytes.");
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists.");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default user role is not configured."));
        User user = new User(
                request.fullName().trim(),
                email,
                request.phone().trim(),
                passwordEncoder.encode(request.password())
        );
        user.addRole(userRole);

        userRepository.saveAndFlush(user);

        addressRepository.save(new Address(
                user,
                request.fullName().trim(),
                request.phone().trim(),
                request.addressLine1().trim(),
                request.city().trim(),
                request.state().trim(),
                request.postalCode().trim(),
                request.country().trim()
        ));
        return createAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new UnauthorizedException("Invalid email or password.");
        }
        String email = normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException exception) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(String email, String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("A bearer token is required.");
        }

        JwtService.TokenClaims claims;
        try {
            claims = jwtService.parse(authorizationHeader.substring(7).trim());
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            throw new UnauthorizedException("Invalid or expired token.");
        }
        if (!claims.subject().equalsIgnoreCase(email)) {
            throw new UnauthorizedException("Token does not belong to the authenticated user.");
        }
        jwtService.revoke(claims);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found."));
        return toUserResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {
        JwtService.IssuedToken token = jwtService.issue(user.getEmail());
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .sorted(Comparator.naturalOrder())
                        .toList()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
