package com.f2pool.controller;

import com.f2pool.common.ApiException;
import com.f2pool.common.JwtTokenUtil;
import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.dto.auth.LoginRequest;
import com.f2pool.dto.auth.RegisterRequest;
import com.f2pool.dto.auth.ResetPasswordByEmailRequest;
import com.f2pool.dto.auth.SendEmailCodeRequest;
import com.f2pool.dto.auth.UpdateLoginPasswordRequest;
import com.f2pool.dto.auth.UpdateWithdrawPasswordRequest;
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

@Api(tags = "User auth API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private IUserAuthService userAuthService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private TokenContextUtil tokenContextUtil;

    @ApiOperation("Register user")
    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        return R.ok(userAuthService.register(request));
    }

    @ApiOperation("Login user")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return R.ok(userAuthService.login(request));
    }

    @ApiOperation("Send email code")
    @PostMapping("/email-code/send")
    public R<Map<String, Object>> sendEmailCode(@RequestBody SendEmailCodeRequest request) {
        return R.ok(userAuthService.sendEmailCode(request));
    }

    @ApiOperation("Reset login password by email code")
    @PostMapping("/password/login/reset-by-email")
    public R<Map<String, Object>> resetPasswordByEmail(@RequestBody ResetPasswordByEmailRequest request) {
        return R.ok(userAuthService.resetPasswordByEmail(request));
    }

    @ApiOperation("Get current user by JWT")
    @GetMapping("/me")
    public R<Map<String, Object>> me(@RequestHeader("Authorization") String authorization) {
        String token = jwtTokenUtil.extractToken(authorization);
        Claims claims = jwtTokenUtil.parseClaims(token);
        Object uid = claims.get("uid");
        if (uid == null) {
            throw ApiException.unauthorized("invalid token: missing uid");
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
        data.put("hasWithdrawPassword", user.getWithdrawPassword() != null && !user.getWithdrawPassword().isBlank());
        data.put("role", claims.get("role"));
        data.put("subject", claims.getSubject());
        data.put("expireAt", claims.getExpiration());
        return R.ok(data);
    }

    @ApiOperation("Update login password")
    @PostMapping("/password/login/update")
    public R<Map<String, Object>> updateLoginPassword(@RequestHeader("Authorization") String authorization,
                                                      @RequestBody UpdateLoginPasswordRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userAuthService.updateLoginPassword(userId, request));
    }

    @ApiOperation("Set or update withdraw password")
    @PostMapping("/password/withdraw/update")
    public R<Map<String, Object>> updateWithdrawPassword(@RequestHeader("Authorization") String authorization,
                                                         @RequestBody UpdateWithdrawPasswordRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userAuthService.updateWithdrawPassword(userId, request));
    }

    @ApiOperation("Get withdraw password status")
    @GetMapping("/password/withdraw/status")
    public R<Map<String, Object>> withdrawPasswordStatus(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(userAuthService.getWithdrawPasswordStatus(userId));
    }

    @ApiOperation("Logout user")
    @PostMapping("/logout")
    public R<Map<String, Object>> logout() {
        Map<String, Object> data = new HashMap<>();
        data.put("loggedOut", true);
        return R.ok(data);
    }
}
