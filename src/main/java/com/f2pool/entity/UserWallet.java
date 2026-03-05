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
    private BigDecimal balanceCny;
    private BigDecimal freezeCny;
    private BigDecimal totalRechargeCny;
    private BigDecimal totalWithdrawCny;
    private Date createTime;
    private Date updateTime;
}
