package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("machine_revenue_withdraw_item")
public class MachineRevenueWithdrawItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long withdrawOrderId;
    private Long machineOrderId;
    private BigDecimal amountBtc;
    private String receiveAddress;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
