package com.f2pool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.f2pool.dto.auth.LoginRequest;
import com.f2pool.dto.auth.RegisterRequest;
import com.f2pool.entity.SysUser;

import java.util.Map;

public interface IUserAuthService extends IService<SysUser> {
    Map<String, Object> register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);
}
