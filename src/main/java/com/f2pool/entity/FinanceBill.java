package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("finance_bill")
public class FinanceBill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String coinSymbol;
    private Integer type; // 1:Revenue, 2:Payout
    private BigDecimal amount;
    private Date createTime;
    private String txId;
}
