package com.f2pool.common;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TokenContextUtil {
    private static final String ADMIN_SESSION_TOKEN_KEY_PREFIX = "f2pool:admin:session:token:";

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public Long requireUserId(String authorization) {
        Claims claims = parseClaims(authorization);
        String role = String.valueOf(claims.get("role"));
        if (!"USER".equalsIgnoreCase(role)) {
            throw ApiException.forbidden("禁止访问：需要用户令牌");
        }
        Object uid = claims.get("uid");
        if (uid == null) {
            throw ApiException.unauthorized("无效令牌：缺少用户标识");
        }
        return Long.valueOf(String.valueOf(uid));
    }

    public Long requireAdminId(String authorization) {
        String token = jwtTokenUtil.extractToken(authorization);
        Claims claims = parseClaims(authorization);
        String role = String.valueOf(claims.get("role"));
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw ApiException.forbidden("禁止访问：需要管理员令牌");
        }
        Object uid = claims.get("uid");
        if (uid == null) {
            throw ApiException.unauthorized("无效令牌：缺少用户标识");
        }
        Long adminId = Long.valueOf(String.valueOf(uid));
        validateAdminSession(adminId, token);
        return adminId;
    }

    private Claims parseClaims(String authorization) {
        String token = jwtTokenUtil.extractToken(authorization);
        return jwtTokenUtil.parseClaims(token);
    }

    private void validateAdminSession(Long adminId, String token) {
        if (adminId == null || !StringUtils.hasText(token)) {
            throw ApiException.unauthorized("无效令牌");
        }
        String key = ADMIN_SESSION_TOKEN_KEY_PREFIX + adminId;
        String latestToken = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(latestToken) || !token.equals(latestToken)) {
            throw ApiException.unauthorized("当前账号在其他地方登录，请重新登录");
        }
    }
}
