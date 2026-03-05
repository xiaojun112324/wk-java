package com.f2pool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.f2pool.dto.auth.AdminLoginRequest;
import com.f2pool.dto.auth.AdminRegisterRequest;
import com.f2pool.entity.AdminUser;

import java.util.Map;

public interface IAdminAuthService extends IService<AdminUser> {
    Map<String, Object> register(AdminRegisterRequest request);

    Map<String, Object> login(AdminLoginRequest request);
}
