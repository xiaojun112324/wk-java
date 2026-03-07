package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("withdraw_order")
public class WithdrawOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String asset;
    private String network;
    private BigDecimal amount;
    private String receiveAddress;
    private Integer status;
    private String auditRemark;
    private Date auditTime;
    private Date createTime;
    private Date updateTime;
}
