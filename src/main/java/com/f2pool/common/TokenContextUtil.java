package com.f2pool.common;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TokenContextUtil {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public Long requireUserId(String authorization) {
        Claims claims = parseClaims(authorization);
        String role = String.valueOf(claims.get("role"));
        if (!"USER".equalsIgnoreCase(role)) {
            throw ApiException.forbidden("forbidden: user token required");
        }
        Object uid = claims.get("uid");
        if (uid == null) {
            throw ApiException.unauthorized("invalid token: uid missing");
        }
        return Long.valueOf(String.valueOf(uid));
    }

    public Long requireAdminId(String authorization) {
        Claims claims = parseClaims(authorization);
        String role = String.valueOf(claims.get("role"));
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw ApiException.forbidden("forbidden: admin token required");
        }
        Object uid = claims.get("uid");
        if (uid == null) {
            throw ApiException.unauthorized("invalid token: uid missing");
        }
        return Long.valueOf(String.valueOf(uid));
    }

    private Claims parseClaims(String authorization) {
        String token = jwtTokenUtil.extractToken(authorization);
        return jwtTokenUtil.parseClaims(token);
    }
}
