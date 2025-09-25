package com.egg.springboot_egg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.egg.springboot_egg.config.GlobalExceptionHandler.BusinessException;
import com.egg.springboot_egg.config.shrioandjwt.JwtUtil;
import com.egg.springboot_egg.entity.User;
import com.egg.springboot_egg.mapper.UserMapper;
import com.egg.springboot_egg.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Logger logger = Logger.getLogger(UserServiceImpl.class.getName());

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<String> getRolesByUserId(Long userId) {
        return userMapper.getRolesByUserId(userId);
    }

    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        return userMapper.getPermissionsByUserId(userId);
    }

    @Override
    public User findById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setRoles(getRolesByUserId(id));
            user.setPermissions(getPermissionsByUserId(id));
        }
        return user;
    }

    // ----------------- ALL 缓存 -----------------
    @Cacheable(value = "userCache", key = "'ALL'", sync = true)
    public List<User> getAllUsers() {
        logger.info("userCache.ALL 缓存未命中，查询数据库");
        return userMapper.selectList(null);
    }

    // ----------------- 单列缓存 -----------------
    @Cacheable(value = "userCache", key = "'usernameList'", sync = true)
    public List<String> getUsernameList() {
        return getAllUsers().stream()
                .map(User::getUsername)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Cacheable(value = "userCache", key = "'emailList'", sync = true)
    public List<String> getEmailList() {
        return getAllUsers().stream()
                .map(User::getEmail)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Cacheable(value = "userCache", key = "'ageList'", sync = true)
    public List<Integer> getAgeList() {
        return getAllUsers().stream()
                .map(User::getAge)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Cacheable(value = "userCache", key = "'genderList'", sync = true)
    public List<Integer> getGenderList() {
        return getAllUsers().stream()
                .map(User::getGender)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    // ----------------- 登录 -----------------
    @Override
    public Map<String, Object> loginUser(String username, String password) {
        User user = findByUsernameOrEmail(username);
        if (user == null) {
            throw new BusinessException(400, "登录失败：用户不存在");
        }
        if (!user.getPassword().equals(password)) {
            throw new BusinessException(400, "登录失败：密码错误");
        }

        String token = JwtUtil.generateToken(user.getUsername(), user.getRoles(), user.getPermissions());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("token", token);
        result.put("message", "登录成功");
        return result;
    }

    @Override
    public User findByUsernameOrEmail(String usernameOrEmail) {
        List<User> users = userMapper.selectList(new QueryWrapper<User>()
                .eq("username", usernameOrEmail)
                .or()
                .eq("email", usernameOrEmail));

        if (users.isEmpty()) {
            return null; // 用户不存在
        }

        // 如果数据库里有重复用户名/邮箱，取第一个即可，打印警告
        if (users.size() > 1) {
            System.out.println("[WARN] 登录检测到重复用户记录，usernameOrEmail=" + usernameOrEmail);
        }

        User user = users.get(0);
        user.setRoles(userMapper.getRolesByUserId(user.getId()));
        user.setPermissions(userMapper.getPermissionsByUserId(user.getId()));
        return user;
    }

    // ----------------- 注册 -----------------
    @Override
    public Map<String, Object> registerUser(User user) {
        validateUser(user, false); // 注册不允许重复，isUpdate=false
        User savedUser = saveUser(user);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", savedUser.getId());
        result.put("message", "注册成功");
        return result;
    }

    // ----------------- 新增 -----------------
    @Override
    @CacheEvict(value = "userCache", allEntries = true)
    public User saveUser(User user) {
        validateUser(user, false); // 新增也不能重复
        boolean result = userMapper.insert(user) > 0;
        if (!result) {
            throw new BusinessException("新增用户失败：数据库插入异常");
        }
        return user;
    }

    // ----------------- 更新 -----------------
    @Override
    @CacheEvict(value = "userCache", allEntries = true)
    public User updateUser(User user) {
        validateUser(user, true); // 更新时排除自身 ID 的重复校验
        boolean result = userMapper.updateById(user) > 0;
        if (!result) {
            throw new BusinessException("更新用户失败：用户不存在或数据库更新异常，ID=" + user.getId());
        }
        return user;
    }

    // ----------------- 删除 -----------------
    @Override
    @CacheEvict(value = "userCache", allEntries = true)
    public boolean deleteUser(Long id) {
        boolean result = userMapper.deleteById(id) > 0;
        if (!result) {
            throw new BusinessException("删除用户失败：用户不存在或数据库删除异常，ID=" + id);
        }
        return true;
    }

    // ----------------- 查询单条用户 -----------------
    @Override
    public User getUserById(Long id) {
        return getAllUsers().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "用户不存在，ID=" + id));
    }

    // ----------------- 复合查询 + 分页 -----------------
    @Override
    public Map<String, Object> queryUsers(Map<String, Object> criteria, int page, int pageSize) {
        List<User> all = getAllUsers();
        List<User> filtered = all.stream().filter(u -> {
            boolean match = true;
            Object username = criteria.get("username");
            if (username != null && !"".equals(username.toString().trim())) {
                match &= u.getUsername().equals(username.toString().trim());
            }
            Object email = criteria.get("email");
            if (email != null && !"".equals(email.toString().trim())) {
                match &= u.getEmail().equals(email.toString().trim());
            }
            Object password = criteria.get("password");
            if (password != null && !"".equals(password.toString().trim())) {
                match &= u.getPassword().equals(password.toString().trim());
            }
            Object age = criteria.get("age");
            if (age != null && !"".equals(age.toString().trim())) {
                match &= u.getAge().equals(Integer.valueOf(age.toString().trim()));
            }
            Object gender = criteria.get("gender");
            if (gender != null && !"".equals(gender.toString().trim())) {
                match &= u.getGender().equals(Integer.valueOf(gender.toString().trim()));
            }
            return match;
        }).collect(Collectors.toList());

        int total = filtered.size();
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<User> pageData = filtered.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("data", pageData);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    // ----------------- 校验方法 -----------------
    private void validateUser(User user, boolean isUpdate) {
        // 必填字段校验
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new BusinessException(400, "操作失败：用户名不能为空");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new BusinessException(400, "操作失败：邮箱不能为空");
        }

        // 唯一性校验：用户名
        User existingUserByName = userMapper.selectOne(new QueryWrapper<User>().eq("username", user.getUsername()));
        if (existingUserByName != null && (!isUpdate || !existingUserByName.getId().equals(user.getId()))) {
            throw new BusinessException(400, "操作失败：用户名已存在");
        }

        // 唯一性校验：邮箱
        User existingUserByEmail = userMapper.selectOne(new QueryWrapper<User>().eq("email", user.getEmail()));
        if (existingUserByEmail != null && (!isUpdate || !existingUserByEmail.getId().equals(user.getId()))) {
            throw new BusinessException(400, "操作失败：邮箱已存在");
        }

        // 可选字段默认值处理
        if (user.getAge() == null) user.setAge(18);
        if (user.getGender() == null) user.setGender(1);
    }

}
