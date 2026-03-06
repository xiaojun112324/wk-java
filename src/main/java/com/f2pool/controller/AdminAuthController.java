package com.f2pool.controller;

import com.f2pool.common.JwtTokenUtil;
import com.f2pool.common.R;
import com.f2pool.dto.auth.AdminLoginRequest;
import com.f2pool.dto.auth.AdminRegisterRequest;
import com.f2pool.entity.AdminUser;
import com.f2pool.mapper.AdminUserMapper;
import com.f2pool.service.IAdminAuthService;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "管理端认证接口")
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired
    private IAdminAuthService adminAuthService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private AdminUserMapper adminUserMapper;

    @ApiOperation("管理员注册（账号/邮箱/密码/系统邀请码）")
    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody AdminRegisterRequest request) {
        return R.ok(adminAuthService.register(request));
    }

    @ApiOperation("管理员登录（账号或邮箱+密码）")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody AdminLoginRequest request) {
        return R.ok(adminAuthService.login(request));
    }

    @ApiOperation("根据 token 获取当前管理员信息")
    @GetMapping("/me")
    public R<Map<String, Object>> me(@RequestHeader("Authorization") String authorization) {
        String token = jwtTokenUtil.extractToken(authorization);
        Claims claims = jwtTokenUtil.parseClaims(token);
        Object uid = claims.get("uid");
        if (uid == null) {
            throw new IllegalArgumentException("invalid token: uid missing");
        }
        Long userId = Long.valueOf(String.valueOf(uid));
        AdminUser user = adminUserMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("admin user not found");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("status", user.getStatus());
        data.put("role", claims.get("role"));
        data.put("subject", claims.getSubject());
        data.put("expireAt", claims.getExpiration());
        return R.ok(data);
    }
}
