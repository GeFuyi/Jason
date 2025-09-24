package com.egg.springboot_egg.mapper;

import com.egg.springboot_egg.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Egg jason
 * @since 2025-09-13
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    @Select("SELECT * FROM chat_message WHERE " +
            "(from_user_id = #{fromUserId} AND to_user_id = #{toUserId}) " +
            "OR (from_user_id = #{toUserId} AND to_user_id = #{fromUserId}) " +
            "ORDER BY create_time ASC")
    List<ChatMessage> selectMessages(Long fromUserId, Long toUserId);
}
