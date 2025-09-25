package com.egg.springboot_egg.mapper;

import com.egg.springboot_egg.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author Egg jason
 * @since 2025-08-16
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    List<String> getRolesByUserId(@Param("userId") Long userId);
    List<String> getPermissionsByUserId(@Param("userId") Long userId);
}
