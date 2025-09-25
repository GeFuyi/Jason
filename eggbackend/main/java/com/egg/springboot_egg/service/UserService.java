package com.egg.springboot_egg.service;

import com.egg.springboot_egg.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 用户表 服务类
 */
public interface UserService extends IService<User> {


    /**
     * 根据用户ID获取角色列表
     * @param userId 用户ID
     * @return 角色名列表
     */
    List<String> getRolesByUserId(Long userId);

    /**
     * 根据用户ID获取权限列表
     * @param userId 用户ID
     * @return 权限名列表
     */
    List<String> getPermissionsByUserId(Long userId);

    /**
     * 根据用户ID查询用户信息，并包含角色和权限
     * @param id 用户ID
     * @return 用户对象
     */
    User findById(Long id);




    // ----------------- 注册 / 登录 -----------------
    Map<String, Object> registerUser(User user);
    Map<String, Object> loginUser(String username, String password);

    // ----------------- CRUD -----------------
    List<User> getAllUsers();
    User getUserById(Long id);
    User saveUser(User user);
    User updateUser(User user);
    boolean deleteUser(Long id);

    // ----------------- 查询用户名或邮箱 -----------------
    User findByUsernameOrEmail(String usernameOrEmail);

    // ----------------- 缓存相关 / 条件查询 -----------------
    /**
     * 条件查询 + 分页
     * @param criteria 过滤条件（username, email, password, age, gender）
     * @param page 当前页
     * @param pageSize 每页数量
     * @return 过滤后的分页用户列表
     */
//    List<User> queryUsers(Map<String, Object> criteria, int page, int pageSize);
    // 查询 + 分页 + 总数
    Map<String, Object> queryUsers(Map<String, Object> criteria, int page, int pageSize);

    /**
     * 单列去重列表（下拉框数据）
     */
    List<String> getUsernameList();
    List<String> getEmailList();
//    List<String> getPasswordList();
    List<Integer> getAgeList();
    List<Integer> getGenderList();
}
