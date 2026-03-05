package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("user_machine_order")
public class UserMachineOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long machineId;
    private String coinSymbol;
    private String machineName;
    private BigDecimal hashrateValue;
    private String hashrateUnit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalInvest;
    private BigDecimal totalHashrateTh;
    private BigDecimal todayRevenueCoin;
    private BigDecimal todayRevenueCny;
    private BigDecimal totalRevenueCoin;
    private BigDecimal totalRevenueCny;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
