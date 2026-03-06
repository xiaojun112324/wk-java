package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.f2pool.common.ApiException;
import com.f2pool.common.JwtTokenUtil;
import com.f2pool.dto.auth.LoginRequest;
import com.f2pool.dto.auth.RegisterRequest;
import com.f2pool.entity.SysUser;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.service.IUserAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserAuthServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements IUserAuthService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public Map<String, Object> register(RegisterRequest request) {
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
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("password must be at least 6 characters");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        long usernameExists = count(new QueryWrapper<SysUser>().eq("username", username));
        if (usernameExists > 0) {
            throw ApiException.conflict("username already exists");
        }

        long emailExists = count(new QueryWrapper<SysUser>().eq("email", email));
        if (emailExists > 0) {
            throw ApiException.conflict("email already exists");
        }

        Long inviterId = null;
        if (StringUtils.hasText(request.getInviteCode())) {
            SysUser inviter = getOne(new QueryWrapper<SysUser>().eq("invite_code", request.getInviteCode()));
            if (inviter == null) {
                throw new IllegalArgumentException("invite code not found");
            }
            inviterId = inviter.getId();
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setInviteCode(generateInviteCode());
        user.setInviterId(inviterId);
        user.setStatus(1);
        save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtTokenUtil.generateToken(user.getId(), user.getUsername(), "USER"));
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenUtil.getExpireSeconds());
        result.put("user", buildUserInfo(user));
        return result;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
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

        SysUser user = getOne(
                new QueryWrapper<SysUser>()
                        .nested(wrapper -> wrapper.eq("username", account).or().eq("email", account))
        );

        if (user == null) {
            throw ApiException.notFound("user not found");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw ApiException.forbidden("account is disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("password is incorrect");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtTokenUtil.generateToken(user.getId(), user.getUsername(), "USER"));
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenUtil.getExpireSeconds());
        result.put("user", buildUserInfo(user));
        return result;
    }

    private Map<String, Object> buildUserInfo(SysUser user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("email", user.getEmail());
        info.put("inviteCode", user.getInviteCode());
        info.put("inviterId", user.getInviterId());
        info.put("status", user.getStatus());
        return info;
    }

    private String generateInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (count(new QueryWrapper<SysUser>().eq("invite_code", code)) > 0);
        return code;
    }
}
