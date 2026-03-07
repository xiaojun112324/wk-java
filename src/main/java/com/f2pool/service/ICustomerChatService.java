package com.f2pool.service;

import java.util.List;
import java.util.Map;

public interface ICustomerChatService {
    Map<String, Object> initUserRoom(Long userId);

    List<Map<String, Object>> listUserMessages(Long userId, Long roomId, Long afterId, Integer limit);

    Map<String, Object> sendUserMessage(Long userId, Long roomId, Integer messageType, String messageContent);

    void markUserRead(Long userId, Long roomId);

    List<Map<String, Object>> listAdminRooms(Long adminId, String keyword);

    List<Map<String, Object>> listAdminMessages(Long adminId, Long roomId, Long afterId, Integer limit);

    Map<String, Object> sendAdminMessage(Long adminId, Long roomId, Integer messageType, String messageContent);

    void markAdminRead(Long adminId, Long roomId);
}
