package com.f2pool.dto.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawSubmitRequest {
    private Long userId;
    private String asset;
    private String network;
    private BigDecimal amount;
    private BigDecimal amountCny;
    private String receiveAddress;
}
