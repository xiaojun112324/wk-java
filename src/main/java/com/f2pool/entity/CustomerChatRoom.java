package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("customer_chat_room")
public class CustomerChatRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long adminId;
    private String lastMessage;
    private Integer lastMessageType;
    private Date lastMessageTime;
    private Integer unreadUser;
    private Integer unreadAdmin;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
