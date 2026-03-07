package com.f2pool.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.f2pool.dto.auth.LoginRequest;
import com.f2pool.dto.auth.RegisterRequest;
import com.f2pool.dto.auth.UpdateLoginPasswordRequest;
import com.f2pool.dto.auth.UpdateWithdrawPasswordRequest;
import com.f2pool.entity.SysUser;

import java.util.Map;

public interface IUserAuthService extends IService<SysUser> {
    Map<String, Object> register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    Map<String, Object> updateLoginPassword(Long userId, UpdateLoginPasswordRequest request);

    Map<String, Object> updateWithdrawPassword(Long userId, UpdateWithdrawPasswordRequest request);

    Map<String, Object> getWithdrawPasswordStatus(Long userId);
}
