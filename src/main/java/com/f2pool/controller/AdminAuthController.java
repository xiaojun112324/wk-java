package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.dto.auth.AdminLoginRequest;
import com.f2pool.dto.auth.AdminRegisterRequest;
import com.f2pool.service.IAdminAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "Admin Auth APIs")
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired
    private IAdminAuthService adminAuthService;

    @ApiOperation("Admin register by username/email/password + registerInviteCode(from sys_config)")
    @PostMapping("/register")
    public R<Map<String, Object>> register(@RequestBody AdminRegisterRequest request) {
        return R.ok(adminAuthService.register(request));
    }

    @ApiOperation("Admin login by username or email + password")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody AdminLoginRequest request) {
        return R.ok(adminAuthService.login(request));
    }
}
