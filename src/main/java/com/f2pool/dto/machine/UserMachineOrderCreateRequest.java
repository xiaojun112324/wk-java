package com.f2pool.dto.machine;

import lombok.Data;

@Data
public class UserMachineOrderCreateRequest {
    private Long userId;
    private Long machineId;
    private Integer quantity;
}
