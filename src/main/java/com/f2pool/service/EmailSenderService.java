package com.f2pool.service;

public interface EmailSenderService {
    void sendVerificationCode(String to, String code, String scene);
}
