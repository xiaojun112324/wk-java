package com.f2pool.dto.auth;

import lombok.Data;

@Data
public class UpdateWithdrawPasswordRequest {
    private String oldPassword;
    private String newPassword;
}
