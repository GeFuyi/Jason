package com.egg.springboot_egg.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author Egg jason
 * @since 2025-09-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
//@ApiModel(value="ChatMessage对象", description="")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long fromUserId;

    private Long toUserId;

    private String content;

    private LocalDateTime createTime;

    @TableField(exist = false)
    private String fromUsername;

    @TableField(exist = false)
    private String toUsername;

}
