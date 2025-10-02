package com.egg.springboot_egg.service;

import com.egg.springboot_egg.entity.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Egg jason
 * @since 2025-09-13
 */
@Mapper
public interface ChatMessageService extends IService<ChatMessage> {
    ChatMessage saveMessage(ChatMessage message);
    List<ChatMessage> getMessages(Long fromUserId, Long toUserId);
}
