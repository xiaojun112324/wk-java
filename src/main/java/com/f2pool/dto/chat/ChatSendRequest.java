package com.f2pool.dto.chat;

import lombok.Data;

@Data
public class ChatSendRequest {
    private Long roomId;
    private Integer messageType;
    private String messageContent;
}
