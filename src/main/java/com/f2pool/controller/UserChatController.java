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

@Api(tags = "用户客服聊天接口")
@RestController
@RequestMapping("/api/chat")
public class UserChatController {

    @Autowired
    private TokenContextUtil tokenContextUtil;
    @Autowired
    private ICustomerChatService customerChatService;

    @ApiOperation("用户初始化客服会话")
    @PostMapping("/room/init")
    public R<Map<String, Object>> initRoom(@RequestHeader("Authorization") String authorization) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(customerChatService.initUserRoom(userId));
    }

    @ApiOperation("用户轮询客服消息")
    @GetMapping("/messages")
    public R<List<Map<String, Object>>> messages(
            @RequestHeader("Authorization") String authorization,
            @ApiParam(value = "会话ID", required = true) @RequestParam Long roomId,
            @ApiParam(value = "从该消息ID之后增量拉取", example = "0") @RequestParam(required = false) Long afterId,
            @ApiParam(value = "单次条数，默认50，最大200", example = "50") @RequestParam(required = false) Integer limit) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(customerChatService.listUserMessages(userId, roomId, afterId, limit));
    }

    @ApiOperation("用户发送消息（文本/图片）")
    @PostMapping("/send")
    public R<Map<String, Object>> send(
            @RequestHeader("Authorization") String authorization,
            @RequestBody ChatSendRequest request) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        return R.ok(customerChatService.sendUserMessage(
                userId,
                request == null ? null : request.getRoomId(),
                request == null ? null : request.getMessageType(),
                request == null ? null : request.getMessageContent()
        ));
    }

    @ApiOperation("用户标记会话已读")
    @PostMapping("/read")
    public R<Boolean> read(
            @RequestHeader("Authorization") String authorization,
            @ApiParam(value = "会话ID", required = true) @RequestParam Long roomId) {
        Long userId = tokenContextUtil.requireUserId(authorization);
        customerChatService.markUserRead(userId, roomId);
        return R.ok(true);
    }
}
