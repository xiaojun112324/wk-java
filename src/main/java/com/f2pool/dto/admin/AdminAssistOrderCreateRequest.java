package com.f2pool.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminAssistOrderCreateRequest {
    private Long userId;
    private String coinSymbol;
    private BigDecimal pCount;
    private BigDecimal totalAmountUsd;
    private BigDecimal usdtPay;
    private BigDecimal usdcPay;
    private String receiveAddress;
}
