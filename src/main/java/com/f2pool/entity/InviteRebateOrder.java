package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("invite_rebate_order")
public class InviteRebateOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long beneficiaryUserId;
    private Long sourceUserId;
    private Long rechargeOrderId;
    private Integer level;
    private BigDecimal sourceRechargeAmountCny;
    private BigDecimal rebateRate;
    private BigDecimal rebateAmountCny;
    private Date createTime;
}
