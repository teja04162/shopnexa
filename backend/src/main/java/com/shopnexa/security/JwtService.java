package com.shopnexa.security;

import com.shopnexa.entity.RevokedToken;
import com.shopnexa.repository.RevokedTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiration;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtService(
            @Value("${app.jwt.secret}") String base64Secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            RevokedTokenRepository revokedTokenRepository
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        if (expirationMs < 1_000) {
            throw new IllegalArgumentException("JWT expiration must be at least one second");
        }
        this.expiration = Duration.ofMillis(expirationMs);
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public IssuedToken issue(String email) {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(expiration).truncatedTo(ChronoUnit.SECONDS);
        String token = Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getId() == null || claims.getSubject() == null || claims.getExpiration() == null) {
            throw new IllegalArgumentException("JWT is missing required claims");
        }
        return new TokenClaims(
                claims.getId(),
                claims.getSubject(),
                claims.getExpiration().toInstant()
        );
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        return revokedTokenRepository.existsById(tokenId);
    }

    @Transactional
    public void revoke(TokenClaims claims) {
        if (!isRevoked(claims.tokenId())) {
            revokedTokenRepository.save(new RevokedToken(claims.tokenId(), claims.expiresAt()));
        }
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    public record TokenClaims(String tokenId, String subject, Instant expiresAt) {
    }
}
