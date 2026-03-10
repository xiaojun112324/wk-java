package com.f2pool.dto.chat;

import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class ChatSendRequest {
    private Long roomId;
    private String messageType;
    private String messageContent;

    public Integer resolveMessageType() {
        if (!StringUtils.hasText(messageType)) {
            return 1;
        }
        String val = messageType.trim().toUpperCase();
        if ("2".equals(val) || "IMAGE".equals(val)) {
            return 2;
        }
        return 1;
    }
}
