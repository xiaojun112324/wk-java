package com.f2pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.f2pool.common.ApiException;
import com.f2pool.entity.AdminUser;
import com.f2pool.entity.CustomerChatMessage;
import com.f2pool.entity.CustomerChatRoom;
import com.f2pool.entity.SysUser;
import com.f2pool.mapper.AdminUserMapper;
import com.f2pool.mapper.CustomerChatMessageMapper;
import com.f2pool.mapper.CustomerChatRoomMapper;
import com.f2pool.mapper.SysUserMapper;
import com.f2pool.service.ICustomerChatService;
import com.f2pool.service.UserFeatureRestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerChatServiceImpl implements ICustomerChatService {
    private static final int SENDER_USER = 1;
    private static final int SENDER_ADMIN = 2;
    private static final int MSG_TEXT = 1;
    private static final int MSG_IMAGE = 2;

    @Autowired
    private CustomerChatRoomMapper roomMapper;
    @Autowired
    private CustomerChatMessageMapper messageMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private AdminUserMapper adminUserMapper;
    @Autowired
    private UserFeatureRestrictionService userFeatureRestrictionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> initUserRoom(Long userId) {
        if (userId == null) {
            throw ApiException.badRequest("userId is required");
        }

        CustomerChatRoom room = roomMapper.selectOne(new QueryWrapper<CustomerChatRoom>()
                .eq("user_id", userId)
                .eq("status", 1)
                .orderByDesc("id")
                .last("limit 1"));
        if (room == null) {
            room = new CustomerChatRoom();
            room.setUserId(userId);
            room.setStatus(1);
            room.setUnreadUser(0);
            room.setUnreadAdmin(0);
            roomMapper.insert(room);
        }
        return roomToMap(room);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> listUserMessages(Long userId, Long roomId, Long afterId, Integer limit) {
        CustomerChatRoom room = requireUserRoom(userId, roomId);
        List<CustomerChatMessage> list = queryMessages(room.getId(), afterId, limit);
        markUserRead(userId, room.getId());
        return list.stream().map(m -> messageToMap(m, SENDER_USER)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendUserMessage(Long userId, Long roomId, Integer messageType, String messageContent) {
        CustomerChatRoom room = requireUserRoom(userId, roomId);
        userFeatureRestrictionService.assertChatSendAllowed(userId);
        int safeType = normalizeMessageType(messageType);
        String safeContent = normalizeContent(messageContent);

        CustomerChatMessage msg = new CustomerChatMessage();
        msg.setRoomId(room.getId());
        msg.setSenderType(SENDER_USER);
        msg.setSenderId(userId);
        msg.setMessageType(safeType);
        msg.setMessageContent(safeContent);
        msg.setIsRead(0);
        messageMapper.insert(msg);

        CustomerChatRoom update = new CustomerChatRoom();
        update.setId(room.getId());
        update.setLastMessage(safeType == MSG_IMAGE ? "[图片]" : safeContent);
        update.setLastMessageType(safeType);
        update.setLastMessageTime(new Date());
        update.setUnreadAdmin((room.getUnreadAdmin() == null ? 0 : room.getUnreadAdmin()) + 1);
        roomMapper.updateById(update);
        return messageToMap(msg, SENDER_USER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markUserRead(Long userId, Long roomId) {
        CustomerChatRoom room = requireUserRoom(userId, roomId);
        messageMapper.update(null, new UpdateWrapper<CustomerChatMessage>()
                .eq("room_id", room.getId())
                .eq("sender_type", SENDER_ADMIN)
                .eq("is_read", 0)
                .set("is_read", 1));
        roomMapper.update(null, new UpdateWrapper<CustomerChatRoom>()
                .eq("id", room.getId())
                .set("unread_user", 0));
    }

    @Override
    public List<Map<String, Object>> listAdminRooms(Long adminId, String keyword) {
        if (adminId == null) {
            throw ApiException.unauthorized("invalid admin token");
        }
        List<CustomerChatRoom> rooms = roomMapper.selectList(new QueryWrapper<CustomerChatRoom>()
                .eq("status", 1)
                .orderByDesc("update_time"));

        List<Map<String, Object>> result = new ArrayList<>();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();

        for (CustomerChatRoom room : rooms) {
            SysUser user = sysUserMapper.selectById(room.getUserId());
            String username = user == null ? "" : safe(user.getUsername());
            String email = user == null ? "" : safe(user.getEmail());
            String inviteCode = user == null ? "" : safe(user.getInviteCode());
            if (!kw.isEmpty()) {
                String merged = (username + "|" + email + "|" + inviteCode).toLowerCase();
                if (!merged.contains(kw)) {
                    continue;
                }
            }
            Map<String, Object> row = roomToMap(room);
            row.put("username", username);
            row.put("email", email);
            row.put("inviteCode", inviteCode);
            result.add(row);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> listAdminMessages(Long adminId, Long roomId, Long afterId, Integer limit) {
        CustomerChatRoom room = requireAdminRoom(adminId, roomId);
        if (room.getAdminId() == null) {
            CustomerChatRoom bind = new CustomerChatRoom();
            bind.setId(room.getId());
            bind.setAdminId(adminId);
            roomMapper.updateById(bind);
        }
        List<CustomerChatMessage> list = queryMessages(room.getId(), afterId, limit);
        markAdminRead(adminId, room.getId());
        return list.stream().map(m -> messageToMap(m, SENDER_ADMIN)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sendAdminMessage(Long adminId, Long roomId, Integer messageType, String messageContent) {
        CustomerChatRoom room = requireAdminRoom(adminId, roomId);
        int safeType = normalizeMessageType(messageType);
        String safeContent = normalizeContent(messageContent);

        if (room.getAdminId() == null) {
            CustomerChatRoom bind = new CustomerChatRoom();
            bind.setId(room.getId());
            bind.setAdminId(adminId);
            roomMapper.updateById(bind);
        }

        CustomerChatMessage msg = new CustomerChatMessage();
        msg.setRoomId(room.getId());
        msg.setSenderType(SENDER_ADMIN);
        msg.setSenderId(adminId);
        msg.setMessageType(safeType);
        msg.setMessageContent(safeContent);
        msg.setIsRead(0);
        messageMapper.insert(msg);

        CustomerChatRoom update = new CustomerChatRoom();
        update.setId(room.getId());
        update.setLastMessage(safeType == MSG_IMAGE ? "[图片]" : safeContent);
        update.setLastMessageType(safeType);
        update.setLastMessageTime(new Date());
        update.setUnreadUser((room.getUnreadUser() == null ? 0 : room.getUnreadUser()) + 1);
        roomMapper.updateById(update);
        return messageToMap(msg, SENDER_ADMIN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAdminRead(Long adminId, Long roomId) {
        CustomerChatRoom room = requireAdminRoom(adminId, roomId);
        if (room.getAdminId() == null) {
            CustomerChatRoom bind = new CustomerChatRoom();
            bind.setId(room.getId());
            bind.setAdminId(adminId);
            roomMapper.updateById(bind);
        }
        messageMapper.update(null, new UpdateWrapper<CustomerChatMessage>()
                .eq("room_id", room.getId())
                .eq("sender_type", SENDER_USER)
                .eq("is_read", 0)
                .set("is_read", 1));
        roomMapper.update(null, new UpdateWrapper<CustomerChatRoom>()
                .eq("id", room.getId())
                .set("unread_admin", 0));
    }

    private CustomerChatRoom requireUserRoom(Long userId, Long roomId) {
        if (userId == null) {
            throw ApiException.unauthorized("invalid user token");
        }
        if (roomId == null) {
            throw ApiException.badRequest("roomId is required");
        }
        CustomerChatRoom room = roomMapper.selectById(roomId);
        if (room == null || room.getStatus() == null || room.getStatus() != 1) {
            throw ApiException.notFound("chat room not found");
        }
        if (!String.valueOf(userId).equals(String.valueOf(room.getUserId()))) {
            throw ApiException.forbidden("chat room does not belong to this user");
        }
        return room;
    }

    private CustomerChatRoom requireAdminRoom(Long adminId, Long roomId) {
        if (adminId == null) {
            throw ApiException.unauthorized("invalid admin token");
        }
        if (roomId == null) {
            throw ApiException.badRequest("roomId is required");
        }
        CustomerChatRoom room = roomMapper.selectById(roomId);
        if (room == null || room.getStatus() == null || room.getStatus() != 1) {
            throw ApiException.notFound("chat room not found");
        }
        return room;
    }

    private List<CustomerChatMessage> queryMessages(Long roomId, Long afterId, Integer limit) {
        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(200, limit));
        QueryWrapper<CustomerChatMessage> qw = new QueryWrapper<CustomerChatMessage>()
                .eq("room_id", roomId)
                .orderByAsc("id")
                .last("limit " + safeLimit);
        if (afterId != null && afterId > 0) {
            qw.gt("id", afterId);
        }
        return messageMapper.selectList(qw);
    }

    private int normalizeMessageType(Integer type) {
        if (type != null && type == MSG_IMAGE) {
            return MSG_IMAGE;
        }
        return MSG_TEXT;
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw ApiException.badRequest("messageContent is required");
        }
        String value = content.trim();
        if (value.length() > 2000) {
            throw ApiException.badRequest("messageContent is too long");
        }
        return value;
    }

    private Map<String, Object> roomToMap(CustomerChatRoom room) {
        Map<String, Object> map = new HashMap<>();
        if (room == null) {
            return map;
        }
        map.put("roomId", room.getId());
        map.put("userId", room.getUserId());
        map.put("adminId", room.getAdminId());
        map.put("lastMessage", room.getLastMessage());
        map.put("lastMessageType", room.getLastMessageType());
        map.put("lastMessageTime", room.getLastMessageTime());
        map.put("unreadUser", room.getUnreadUser() == null ? 0 : room.getUnreadUser());
        map.put("unreadAdmin", room.getUnreadAdmin() == null ? 0 : room.getUnreadAdmin());
        map.put("status", room.getStatus());
        map.put("createTime", room.getCreateTime());
        map.put("updateTime", room.getUpdateTime());
        if (room.getAdminId() != null) {
            AdminUser adminUser = adminUserMapper.selectById(room.getAdminId());
            if (adminUser != null) {
                map.put("adminUsername", adminUser.getUsername());
            }
        }
        return map;
    }

    private Map<String, Object> messageToMap(CustomerChatMessage msg, int viewerSenderType) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", msg.getId());
        map.put("roomId", msg.getRoomId());
        map.put("senderType", msg.getSenderType());
        map.put("senderId", msg.getSenderId());
        map.put("messageType", msg.getMessageType());
        map.put("messageContent", msg.getMessageContent());
        map.put("isRead", msg.getIsRead() != null && msg.getIsRead() == 1);
        map.put("createTime", msg.getCreateTime());
        map.put("isSelf", msg.getSenderType() != null && msg.getSenderType() == viewerSenderType);
        return map;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
