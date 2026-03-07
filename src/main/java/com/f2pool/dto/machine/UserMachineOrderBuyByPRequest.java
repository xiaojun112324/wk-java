package com.f2pool.dto.machine;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserMachineOrderBuyByPRequest {
    private Long userId;
    private String coinSymbol;
    private BigDecimal pCount;
    private BigDecimal totalAmountUsd;
    private BigDecimal usdtPay;
    private BigDecimal usdcPay;
}
