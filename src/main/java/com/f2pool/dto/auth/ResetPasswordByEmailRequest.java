package com.f2pool.dto.auth;

import lombok.Data;

@Data
public class ResetPasswordByEmailRequest {
    private String account;
    private String emailCode;
    private String newPassword;
}
