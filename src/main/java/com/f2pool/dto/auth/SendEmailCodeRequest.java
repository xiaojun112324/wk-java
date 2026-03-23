package com.f2pool.dto.auth;

import lombok.Data;

@Data
public class SendEmailCodeRequest {
    private String scene;
    private String email;
    private String account;
}
