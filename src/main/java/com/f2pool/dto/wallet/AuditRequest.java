package com.f2pool.dto.wallet;

import lombok.Data;

@Data
public class AuditRequest {
    private Integer status; // 1 approve, 2 reject
    private String remark;
}
