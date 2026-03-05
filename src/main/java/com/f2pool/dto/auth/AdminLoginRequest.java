package com.f2pool.dto.auth;

import lombok.Data;

@Data
public class AdminLoginRequest {
    private String account;
    private String password;
}
