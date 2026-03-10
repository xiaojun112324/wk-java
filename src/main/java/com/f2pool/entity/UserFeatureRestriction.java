package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_feature_restriction")
public class UserFeatureRestriction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer disableLogin;
    private Integer disableRecharge;
    private Integer disableWithdraw;
    private Integer disableOrder;
    private Integer disableSellRecover;
    private Integer disableRevenueWithdraw;
    private Integer disableChatSend;
    private String remark;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
