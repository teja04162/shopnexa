package com.shopnexa.security;

import com.shopnexa.repository.RevokedTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "bG9jYWwtb25seS1zaG9wbmV4YS1qd3Qta2V5LTMyaHl0ZXM=";

    @Test
    void issuedTokenContainsTheExpectedIdentityAndExpiry() {
        JwtService jwtService = new JwtService(TEST_SECRET, 900_000, mock(RevokedTokenRepository.class));

        JwtService.IssuedToken issued = jwtService.issue("shopper@example.com");
        JwtService.TokenClaims claims = jwtService.parse(issued.value());

        assertEquals("shopper@example.com", claims.subject());
        assertEquals(issued.expiresAt(), claims.expiresAt());
        org.junit.jupiter.api.Assertions.assertNotNull(claims.tokenId());
    }

    @Test
    void parserRejectsExpiredAndTamperedTokens() throws InterruptedException {
        JwtService shortLivedService = new JwtService(TEST_SECRET, 1_000, mock(RevokedTokenRepository.class));
        String expiredToken = shortLivedService.issue("shopper@example.com").value();
        Thread.sleep(1_100);

        assertThrows(RuntimeException.class, () -> shortLivedService.parse(expiredToken));

        JwtService jwtService = new JwtService(TEST_SECRET, 900_000, mock(RevokedTokenRepository.class));
        String tamperedToken = jwtService.issue("shopper@example.com").value() + "invalid";
        assertThrows(RuntimeException.class, () -> jwtService.parse(tamperedToken));
    }
}
