package com.egg.springboot_egg.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author Egg jason
 * @since 2025-08-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String password;

    private Integer age;

    private Integer gender;

    @TableField(exist = false)
    private List<String> roles;       // 用户拥有的角色

    @TableField(exist = false)
    private List<String> permissions; // 用户拥有的权限

}
