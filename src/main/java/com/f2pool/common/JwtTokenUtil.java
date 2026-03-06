package com.f2pool.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expire-seconds:604800}")
    private long expireSeconds;

    public String generateToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expireSeconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(buildKey())
                .compact();
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public Claims parseClaims(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("token is required");
        }
        try {
            return Jwts.parser()
                    .verifyWith(buildKey())
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
        } catch (JwtException e) {
            throw new IllegalArgumentException("invalid token");
        }
    }

    public String extractToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        String value = authorizationHeader.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            value = value.substring(7).trim();
        }
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("token is required");
        }
        return value;
    }

    private SecretKey buildKey() {
        String secret = StringUtils.hasText(jwtSecret)
                ? jwtSecret
                : "change-this-to-a-very-long-random-secret-key-please-1234567890";
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            for (int i = bytes.length; i < padded.length; i++) {
                padded[i] = '0';
            }
            bytes = padded;
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
