package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String password;
    private String withdrawPassword;
    private String inviteCode;
    private Long inviterId;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
