package com.f2pool.dto.config;

import lombok.Data;

@Data
public class SysConfigUpdateRequest {
    private String configValue;
    private Integer status;
    private String remark;
}
