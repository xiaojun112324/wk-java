package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("finance_account")
public class FinanceAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String coinSymbol;
    private BigDecimal balance;
    private BigDecimal totalRevenue;
    private BigDecimal totalPaid;
    private String walletAddress;
    private BigDecimal minPayout;
}
