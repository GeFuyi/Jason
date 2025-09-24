package com.egg.springboot_egg.service.upclass;

import com.egg.springboot_egg.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RedisOfflineMessageService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 群聊 Redis key
    private static final String GROUP_CHAT_KEY = "offline:msg:group";

    public RedisOfflineMessageService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        // ✅ 注册 JavaTimeModule 支持 LocalDateTime
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    // ===== 私聊离线消息 =====
    public void saveOfflineMessage(String userId, ChatMessage message) {
        try {
            String key = "offline:msg:" + userId;
            String value = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(key, value); // 队列形式存储
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ChatMessage> getOfflineMessages(String userId) {
        List<ChatMessage> messages = new ArrayList<>();
        try {
            String key = "offline:msg:" + userId;
            List<String> list = redisTemplate.opsForList().range(key, 0, -1);
            redisTemplate.delete(key); // 读取完就删除
            if (list != null) {
                for (String json : list) {
                    messages.add(objectMapper.readValue(json, ChatMessage.class));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    // ===== 群聊消息 =====
    public void saveGroupMessage(ChatMessage message) {
        try {
            String value = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(GROUP_CHAT_KEY, value);
            // 限制队列长度，比如只保留最新 100 条
            redisTemplate.opsForList().trim(GROUP_CHAT_KEY, -100, -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ChatMessage> getGroupMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        try {
            List<String> list = redisTemplate.opsForList().range(GROUP_CHAT_KEY, 0, -1);
            if (list != null) {
                for (String json : list) {
                    messages.add(objectMapper.readValue(json, ChatMessage.class));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }
}
