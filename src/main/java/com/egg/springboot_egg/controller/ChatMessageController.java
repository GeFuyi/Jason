package com.egg.springboot_egg.controller;

import com.egg.springboot_egg.GlobalExceptionHandler.Result;
import com.egg.springboot_egg.entity.ChatMessage;
import com.egg.springboot_egg.service.ChatMessageService;
import com.egg.springboot_egg.config.aspectandeventlistener.WebSocketEventListener;
import com.egg.springboot_egg.service.upclass.RedisOfflineMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final WebSocketEventListener webSocketEventListener;
    private final RedisOfflineMessageService redisOfflineMessageService;

    // ===== WebSocket 消息发送 =====
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message) {
        message.setCreateTime(LocalDateTime.now());
        chatMessageService.saveMessage(message);

        System.out.println("[ChatMessageController] 收到消息: " + message);

        if (message.getToUserId() == null) {
            // 群聊
            messagingTemplate.convertAndSend("/topic/group", message);
            redisOfflineMessageService.saveGroupMessage(message); // 存群聊 Redis
            System.out.println("[ChatMessageController] 群聊消息发送并存 Redis");
        } else {
            // 私聊
            String toUserId = message.getToUserId().toString();
            if (webSocketEventListener.isOnline(toUserId)) {
                String privateTopic = "/topic/private-" + toUserId;
                messagingTemplate.convertAndSend(privateTopic, message);
                System.out.println("[ChatMessageController] 私聊消息发送: " + privateTopic);
            } else {
                // 离线存 Redis
                redisOfflineMessageService.saveOfflineMessage(toUserId, message);
                System.out.println("[ChatMessageController] 用户离线，消息存 Redis: " + message.getContent());
            }
        }
    }

    // ===== 拉取离线私聊消息 =====
    @GetMapping("/user/offline-msg")
    public ResponseEntity<Result<List<ChatMessage>>> getOfflineMessages(@RequestParam String userId) {
        List<ChatMessage> messages = redisOfflineMessageService.getOfflineMessages(userId);
        return ResponseEntity.ok(Result.ok(messages));
    }

    // ===== 拉取群聊消息 =====
    @GetMapping("/chat/group-msg")
    public ResponseEntity<Result<List<ChatMessage>>> getGroupMessages() {
        List<ChatMessage> messages = redisOfflineMessageService.getGroupMessages();
        return ResponseEntity.ok(Result.ok(messages));
    }

    // ===== 获取在线用户 =====
    @GetMapping("/user/online")
    public ResponseEntity<Result<Set<String>>> getOnlineUsers() {
        Set<String> users = webSocketEventListener.getOnlineUsers();
        return ResponseEntity.ok(Result.ok(users));
    }
}
