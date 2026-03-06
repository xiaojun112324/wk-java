package com.f2pool.controller;

import com.f2pool.common.ApiException;
import com.f2pool.common.JwtTokenUtil;
import com.f2pool.common.R;
import com.f2pool.dto.auth.LoginRequest;
import com.f2pool.dto.auth.RegisterRequest;
import com.f2pool.entity.SysUser;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.service.IUserAuthService;
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

@Api(tags = "用户认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private IUserAuthService userAuthService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private SysUserMapper sysUserMapper;

    @ApiOperation("用户注册（账号/邮箱/密码，可选邀请码）")
    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        return R.ok(userAuthService.register(request));
    }

    @ApiOperation("用户登录（账号或邮箱+密码）")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return R.ok(userAuthService.login(request));
    }

    @ApiOperation("根据 token 获取当前用户信息")
    @GetMapping("/me")
    public R<Map<String, Object>> me(@RequestHeader("Authorization") String authorization) {
        String token = jwtTokenUtil.extractToken(authorization);
        Claims claims = jwtTokenUtil.parseClaims(token);
        Object uid = claims.get("uid");
        if (uid == null) {
            throw ApiException.unauthorized("invalid token: uid missing");
        }
        Long userId = Long.valueOf(String.valueOf(uid));
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("user not found");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("inviteCode", user.getInviteCode());
        data.put("inviterId", user.getInviterId());
        data.put("status", user.getStatus());
        data.put("role", claims.get("role"));
        data.put("subject", claims.getSubject());
        data.put("expireAt", claims.getExpiration());
        return R.ok(data);
    }
}
