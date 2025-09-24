package com.egg.springboot_egg.service.impl;

import com.egg.springboot_egg.GlobalExceptionHandler.BusinessException;
import com.egg.springboot_egg.entity.ChatMessage;
import com.egg.springboot_egg.mapper.ChatMessageMapper;
import com.egg.springboot_egg.service.ChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Egg
 * @since 2025-09-13
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageServiceImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public ChatMessage saveMessage(ChatMessage message) {
        int rows = chatMessageMapper.insert(message);
        if (rows <= 0) {
            throw new BusinessException("发送消息失败：数据库插入异常");
        }
        return message;
    }

    @Override
    public List<ChatMessage> getMessages(Long fromUserId, Long toUserId) {
        List<ChatMessage> messages = chatMessageMapper.selectMessages(fromUserId, toUserId);
        if (messages == null) {
            throw new BusinessException("获取聊天记录失败：数据库查询异常");
        }
        return messages;
    }
}
