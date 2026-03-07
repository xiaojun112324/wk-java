package com.f2pool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("customer_chat_message")
public class CustomerChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Integer senderType;
    private Long senderId;
    private Integer messageType;
    private String messageContent;
    private Integer isRead;
    private Date createTime;
}
