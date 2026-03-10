package com.f2pool.controller;

import com.f2pool.common.R;
import com.f2pool.common.TokenContextUtil;
import com.f2pool.dto.chat.ChatSendRequest;
import com.f2pool.service.ICustomerChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "管理后台客服聊天接口")
@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {

    @Autowired
    private TokenContextUtil tokenContextUtil;
    @Autowired
    private ICustomerChatService customerChatService;

    @ApiOperation("管理员会话列表")
    @GetMapping("/rooms")
    public R<List<Map<String, Object>>> rooms(
            @RequestHeader("Authorization") String authorization,
            @ApiParam(value = "搜索用户账号/邮箱/邀请码") @RequestParam(required = false) String keyword) {
        Long adminId = tokenContextUtil.requireAdminId(authorization);
        return R.ok(customerChatService.listAdminRooms(adminId, keyword));
    }

    @ApiOperation("管理员拉取会话消息")
    @GetMapping("/messages")
    public R<List<Map<String, Object>>> messages(
            @RequestHeader("Authorization") String authorization,
            @ApiParam(value = "会话ID", required = true) @RequestParam Long roomId,
            @ApiParam(value = "从该消息ID之后增量拉取", example = "0") @RequestParam(required = false) Long afterId,
            @ApiParam(value = "单次条数，默认50，最大200", example = "50") @RequestParam(required = false) Integer limit) {
        Long adminId = tokenContextUtil.requireAdminId(authorization);
        return R.ok(customerChatService.listAdminMessages(adminId, roomId, afterId, limit));
    }

    @ApiOperation("管理员发送消息（文本/图片）")
    @PostMapping("/send")
    public R<Map<String, Object>> send(
            @RequestHeader("Authorization") String authorization,
            @RequestBody ChatSendRequest request) {
        Long adminId = tokenContextUtil.requireAdminId(authorization);
        return R.ok(customerChatService.sendAdminMessage(
                adminId,
                request == null ? null : request.getRoomId(),
                request == null ? null : request.resolveMessageType(),
                request == null ? null : request.getMessageContent()
        ));
    }

    @ApiOperation("管理员标记会话已读")
    @PostMapping("/read")
    public R<Boolean> read(
            @RequestHeader("Authorization") String authorization,
            @ApiParam(value = "会话ID", required = true) @RequestParam Long roomId) {
        Long adminId = tokenContextUtil.requireAdminId(authorization);
        customerChatService.markAdminRead(adminId, roomId);
        return R.ok(true);
    }
}
