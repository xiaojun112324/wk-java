package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.common.ApiException;
import com.f2pool.common.JwtTokenUtil;
import com.f2pool.dto.auth.AdminLoginRequest;
import com.f2pool.dto.auth.AdminRegisterRequest;
import com.f2pool.entity.AdminUser;
import com.f2pool.entity.SysConfig;
import com.f2pool.mapper.AdminUserMapper;
import com.f2pool.mapper.SysConfigMapper;
import com.f2pool.service.IAdminAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminAuthServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements IAdminAuthService {

    private static final String ADMIN_REGISTER_INVITE_CODE_KEY = "admin_register_invite_code";

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private SysConfigMapper sysConfigMapper;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public Map<String, Object> register(AdminRegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("username is required");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new IllegalArgumentException("email is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("password is required");
        }
        if (!StringUtils.hasText(request.getRegisterInviteCode())) {
            throw new IllegalArgumentException("registerInviteCode is required");
        }
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("password must be at least 6 characters");
        }

        String expectedInviteCode = getAdminRegisterInviteCode();
        if (!expectedInviteCode.equals(request.getRegisterInviteCode().trim())) {
            throw ApiException.forbidden("register invite code is incorrect");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        long usernameExists = count(new QueryWrapper<AdminUser>().eq("username", username));
        if (usernameExists > 0) {
            throw ApiException.conflict("username already exists");
        }

        long emailExists = count(new QueryWrapper<AdminUser>().eq("email", email));
        if (emailExists > 0) {
            throw ApiException.conflict("email already exists");
        }

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtTokenUtil.generateToken(user.getId(), user.getUsername(), "ADMIN"));
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenUtil.getExpireSeconds());
        result.put("user", buildUserInfo(user));
        return result;
    }

    @Override
    public Map<String, Object> login(AdminLoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (!StringUtils.hasText(request.getAccount())) {
            throw new IllegalArgumentException("account is required");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("password is required");
        }

        String account = request.getAccount().trim();
        AdminUser user = getOne(
                new QueryWrapper<AdminUser>()
                        .nested(wrapper -> wrapper.eq("username", account).or().eq("email", account))
        );

        if (user == null) {
            throw ApiException.notFound("admin user not found");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw ApiException.forbidden("admin account is disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("password is incorrect");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtTokenUtil.generateToken(user.getId(), user.getUsername(), "ADMIN"));
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenUtil.getExpireSeconds());
        result.put("user", buildUserInfo(user));
        return result;
    }

    private String getAdminRegisterInviteCode() {
        SysConfig config = sysConfigMapper.selectOne(
                new QueryWrapper<SysConfig>()
                        .eq("config_key", ADMIN_REGISTER_INVITE_CODE_KEY)
                        .eq("status", 1)
        );
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            throw new IllegalArgumentException("admin register invite code is not configured");
        }
        return config.getConfigValue().trim();
    }

    private Map<String, Object> buildUserInfo(AdminUser user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("email", user.getEmail());
        info.put("status", user.getStatus());
        return info;
    }
}
