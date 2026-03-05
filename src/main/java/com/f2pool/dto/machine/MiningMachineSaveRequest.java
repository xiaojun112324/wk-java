package com.f2pool.dto.machine;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MiningMachineSaveRequest {
    private String name;
    private String coinSymbol;
    private BigDecimal hashrateValue;
    private String hashrateUnit;
    private BigDecimal pricePerUnit;
    private Integer lockDays;
    private Integer status;
}
