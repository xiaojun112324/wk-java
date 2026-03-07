package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("user_wallet")
public class UserWallet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal usdtBalance;
    private BigDecimal usdcBalance;
    private BigDecimal btcBalance;
    private BigDecimal usdtFreeze;
    private BigDecimal usdcFreeze;
    private BigDecimal btcFreeze;
    private BigDecimal totalRechargeUsdt;
    private BigDecimal totalRechargeUsdc;
    private BigDecimal totalRechargeBtc;
    private BigDecimal totalWithdrawUsdt;
    private BigDecimal totalWithdrawUsdc;
    private BigDecimal totalWithdrawBtc;
    private Date createTime;
    private Date updateTime;
}
