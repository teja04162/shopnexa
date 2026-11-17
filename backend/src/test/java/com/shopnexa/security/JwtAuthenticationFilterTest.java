package com.shopnexa.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final AuthenticationEntryPoint authenticationEntryPoint = mock(AuthenticationEntryPoint.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtService, userDetailsService, authenticationEntryPoint);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesTheResolvedUser() throws Exception {
        JwtService.TokenClaims claims =
                new JwtService.TokenClaims("token-id", "shopper@example.com", Instant.now().plusSeconds(60));
        when(jwtService.parse("valid-token")).thenReturn(claims);
        when(jwtService.isRevoked("token-id")).thenReturn(false);
        when(userDetailsService.loadUserByUsername("shopper@example.com"))
                .thenReturn(User.withUsername("shopper@example.com")
                        .password("not-used")
                        .authorities("ROLE_USER")
                        .build());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertEquals(
                "shopper@example.com",
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
        assertEquals(
                "ROLE_USER",
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority()
        );
    }

    @Test
    void revokedTokenIsRejectedBeforeTheRequestContinues() throws Exception {
        when(jwtService.parse("revoked-token")).thenReturn(
                new JwtService.TokenClaims("token-id", "shopper@example.com", Instant.now().plusSeconds(60))
        );
        when(jwtService.isRevoked("token-id")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer revoked-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilterInternal(request, response, chain);

        verify(authenticationEntryPoint).commence(
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.eq(response),
                org.mockito.ArgumentMatchers.any()
        );
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
