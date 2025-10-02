package com.egg.springboot_egg.controller;

import com.egg.springboot_egg.config.GlobalExceptionHandler.Result;
import com.egg.springboot_egg.entity.ChatMessage;
import com.egg.springboot_egg.entity.User;
import com.egg.springboot_egg.service.ChatMessageService;
import com.egg.springboot_egg.service.UserService;
import com.egg.springboot_egg.config.aspectandeventlistener.WebSocketEventListener;
import com.egg.springboot_egg.service.upclass.RedisOfflineMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final UserService userService;
    private final WebSocketEventListener webSocketEventListener;
    private final RedisOfflineMessageService redisOfflineMessageService;

    // ===== WebSocket 消息发送 =====
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage message) {
        message.setCreateTime(LocalDateTime.now());

        // 设置发送者 userId（fromUserId）
        if (message.getFromUsername() != null) {
            User fromUser = userService.findByUsername(message.getFromUsername());
            if (fromUser != null) {
                message.setFromUserId(fromUser.getId());
            } else {
                System.out.println("[ChatMessageController] 发送者不存在: " + message.getFromUsername());
                return; // 无效消息，不处理
            }
        }

        // 设置接收者 userId（toUserId），私聊消息使用
        if (message.getToUsername() != null) {
            User toUser = userService.findByUsername(message.getToUsername());
            if (toUser != null) {
                message.setToUserId(toUser.getId());
            } else {
                System.out.println("[ChatMessageController] 接收者不存在: " + message.getToUsername());
            }
        }

        // 保存到数据库
        chatMessageService.saveMessage(message);
        System.out.println("[ChatMessageController] 收到消息: " + message);

        // 群聊消息
        if (message.getToUsername() == null) {
            messagingTemplate.convertAndSend("/topic/group", message);
            redisOfflineMessageService.saveGroupMessage(message);
            System.out.println("[ChatMessageController] 群聊消息发送并存 Redis");
        } else {
            // 私聊消息
            String toUsername = message.getToUsername();
            if (toUsername != null && webSocketEventListener.isOnline(toUsername)) {
                String privateTopic = "/topic/private-" + toUsername;
                messagingTemplate.convertAndSend(privateTopic, message);
                System.out.println("[ChatMessageController] 私聊消息发送: " + privateTopic);
            } else if (toUsername != null) {
                // 用户离线，存 Redis（使用 userId）
                User toUser = userService.findByUsername(toUsername);
                if (toUser != null) {
                    redisOfflineMessageService.saveOfflineMessage(toUser.getId().toString(), message);
                    System.out.println("[ChatMessageController] 用户离线，消息存 Redis: " + message.getContent());
                }
            }
        }
    }

    // ===== 拉取离线私聊消息 =====
    @GetMapping("/user/offline-msg")
    public ResponseEntity<Result<List<ChatMessage>>> getOfflineMessages(@RequestParam String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.ok(Result.ok(new ArrayList<>()));
        }
        List<ChatMessage> messages = redisOfflineMessageService.getOfflineMessages(user.getId().toString());
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
