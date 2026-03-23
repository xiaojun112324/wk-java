package com.f2pool.dto.auth;

import lombok.Data;

@Data
public class UpdateLoginPasswordRequest {
    private String emailCode;
    private String newPassword;
}
