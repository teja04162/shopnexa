package com.shopnexa.service.impl;

import com.shopnexa.dto.RegisterRequest;
import com.shopnexa.entity.Address;
import com.shopnexa.entity.Role;
import com.shopnexa.entity.User;
import com.shopnexa.exception.BadRequestException;
import com.shopnexa.repository.AddressRepository;
import com.shopnexa.repository.RoleRepository;
import com.shopnexa.repository.UserRepository;
import com.shopnexa.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registrationNormalizesEmailHashesPasswordAndAssignsOnlyUserRole() {
        Role userRole = org.mockito.Mockito.mock(Role.class);
        when(userRole.getName()).thenReturn("ROLE_USER");
        when(userRepository.existsByEmailIgnoreCase("shopper@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("strong-password-123")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.issue("shopper@example.com"))
                .thenReturn(new JwtService.IssuedToken("signed-token", Instant.now().plusSeconds(900)));

        authService.register(validRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("shopper@example.com", savedUser.getEmail());
        assertEquals("bcrypt-hash", savedUser.getPasswordHash());
        assertEquals(1, savedUser.getRoles().size());
        assertEquals("ROLE_USER", savedUser.getRoles().iterator().next().getName());

        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(addressCaptor.capture());
        assertEquals(savedUser, addressCaptor.getValue().getUser());
    }

    @Test
    void registrationRejectsMismatchedPasswordsBeforeWritingAnything() {
        RegisterRequest request = new RegisterRequest(
                "Shopper", "shopper@example.com", "555-0123",
                "strong-password-123", "different-password",
                "1 Main Street", "Springfield", "State", "12345", "US"
        );

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(addressRepository, never()).save(any(Address.class));
    }

    private RegisterRequest validRequest() {
        return new RegisterRequest(
                "Shopper", "  Shopper@Example.COM  ", "555-0123",
                "strong-password-123", "strong-password-123",
                "1 Main Street", "Springfield", "State", "12345", "US"
        );
    }

}
