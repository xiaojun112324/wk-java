package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_receive_address")
public class UserReceiveAddress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String network;
    private String receiveAddress;
    private String remark;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
