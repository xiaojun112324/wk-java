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

    private static final String ROLE_USER = "USER";
    private static final long DEFAULT_USER_EXPIRE_SECONDS = 30L * 24 * 60 * 60;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expire-seconds:604800}")
    private long expireSeconds;

    @Value("${jwt.user-expire-seconds:2592000}")
    private long userExpireSeconds;

    public String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, resolveExpireSeconds(role));
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public long getExpireSeconds(String role) {
        return resolveExpireSeconds(role);
    }

    public Claims parseClaims(String token) {
        if (!StringUtils.hasText(token)) {
            throw ApiException.unauthorized("令牌不能为空");
        }
        try {
            return Jwts.parser()
                    .verifyWith(buildKey())
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
        } catch (JwtException e) {
            throw ApiException.unauthorized("无效令牌");
        }
    }

    public String extractToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw ApiException.unauthorized("请求头缺少授权信息");
        }
        String value = authorizationHeader.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            value = value.substring(7).trim();
        }
        if (!StringUtils.hasText(value)) {
            throw ApiException.unauthorized("令牌不能为空");
        }
        return value;
    }

    private String generateToken(Long userId, String username, String role, long ttlSeconds) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(ttlSeconds);

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

    private long resolveExpireSeconds(String role) {
        if (ROLE_USER.equalsIgnoreCase(role)) {
            return userExpireSeconds > 0 ? userExpireSeconds : DEFAULT_USER_EXPIRE_SECONDS;
        }
        return expireSeconds;
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
