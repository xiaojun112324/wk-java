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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminAuthServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements IAdminAuthService {

    private static final String ADMIN_REGISTER_INVITE_CODE_KEY = "admin_register_invite_code";
    private static final String ADMIN_SESSION_TOKEN_KEY_PREFIX = "f2pool:admin:session:token:";

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private SysConfigMapper sysConfigMapper;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Map<String, Object> register(AdminRegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (!StringUtils.hasText(request.getRegisterInviteCode())) {
            throw new IllegalArgumentException("注册邀请码不能为空");
        }
        if (request.getPassword().length() < 6) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }

        String expectedInviteCode = getAdminRegisterInviteCode();
        if (!expectedInviteCode.equals(request.getRegisterInviteCode().trim())) {
            throw ApiException.forbidden("注册邀请码错误");
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
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), "ADMIN");
        storeAdminSessionToken(user.getId(), token);
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenUtil.getExpireSeconds());
        result.put("user", buildUserInfo(user));
        return result;
    }

    @Override
    public Map<String, Object> login(AdminLoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getAccount())) {
            throw new IllegalArgumentException("账号不能为空");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }

        String account = request.getAccount().trim();
        AdminUser user = getOne(
                new QueryWrapper<AdminUser>()
                        .nested(wrapper -> wrapper.eq("username", account).or().eq("email", account))
        );

        if (user == null) {
            throw ApiException.notFound("管理员不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw ApiException.forbidden("管理员账号已禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.unauthorized("密码错误");
        }

        Map<String, Object> result = new HashMap<>();
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), "ADMIN");
        storeAdminSessionToken(user.getId(), token);
        result.put("token", token);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", jwtTokenUtil.getExpireSeconds());
        result.put("user", buildUserInfo(user));
        return result;
    }

    private void storeAdminSessionToken(Long adminId, String token) {
        if (adminId == null || !StringUtils.hasText(token)) {
            return;
        }
        String key = ADMIN_SESSION_TOKEN_KEY_PREFIX + adminId;
        stringRedisTemplate.opsForValue().set(key, token, jwtTokenUtil.getExpireSeconds(), java.util.concurrent.TimeUnit.SECONDS);
    }

    private String getAdminRegisterInviteCode() {
        SysConfig config = sysConfigMapper.selectOne(
                new QueryWrapper<SysConfig>()
                        .eq("config_key", ADMIN_REGISTER_INVITE_CODE_KEY)
                        .eq("status", 1)
        );
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            throw new IllegalArgumentException("未配置管理员注册邀请码");
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
