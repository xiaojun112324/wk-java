package com.f2pool.service;

import com.f2pool.entity.SysUser;

public interface EmailVerificationService {
    void sendRegisterCode(String email);

    void sendResetPasswordCode(String account);

    void verifyRegisterCode(String email, String code);

    SysUser verifyResetPasswordCode(String account, String code);
}
