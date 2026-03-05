package com.f2pool.dto.auth;

import lombok.Data;

@Data
public class LoginRequest {
    private String account;
    private String password;
}
