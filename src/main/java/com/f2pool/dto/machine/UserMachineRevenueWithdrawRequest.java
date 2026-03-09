package com.f2pool.dto.machine;

import lombok.Data;

import java.util.List;

@Data
public class UserMachineRevenueWithdrawRequest {
    private Long userId;
    private String receiveAddress;
    private List<Long> orderIds;
}
