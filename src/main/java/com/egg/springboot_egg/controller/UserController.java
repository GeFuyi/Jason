package com.egg.springboot_egg.controller;


import com.egg.springboot_egg.config.GlobalExceptionHandler.Result;
import com.egg.springboot_egg.entity.User;
import com.egg.springboot_egg.service.UserService;
import com.egg.springboot_egg.service.upclass.UserQueryService;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserQueryService userQueryService;

    // ---------- 注册 / 登录 ----------
    @PostMapping("/register")
    public ResponseEntity<Result<Map<String, Object>>> register(@RequestBody User user) {
        Map<String, Object> res = userService.registerUser(user);
        return ResponseEntity.ok(Result.ok(res));
    }

    @PostMapping("/login")
    public ResponseEntity<Result<Map<String, Object>>> login(@RequestBody User loginUser) {
        Map<String, Object> res = userService.loginUser(loginUser.getUsername(), loginUser.getPassword());
        return ResponseEntity.ok(Result.ok(res));
    }

    // ---------- CRUD ----------
    @GetMapping
    @RequiresAuthentication
    public ResponseEntity<Result<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(Result.ok(users));
    }

    @GetMapping("/{id}")
    @RequiresAuthentication
    public ResponseEntity<Result<User>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(Result.ok(user));
    }

    @PostMapping
    @RequiresRoles("admin")
    @RequiresPermissions("user:create")
    public ResponseEntity<Result<User>> createUser(@RequestBody User user) {
        User saved = userService.saveUser(user);
        return ResponseEntity.ok(Result.ok(saved));
    }

    @PutMapping("/{id}")
    @RequiresPermissions("user:update")
    public ResponseEntity<Result<User>> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        User updated = userService.updateUser(user);
        return ResponseEntity.ok(Result.ok(updated));
    }

    @DeleteMapping("/{id}")
    @RequiresRoles("admin")
    @RequiresPermissions("user:delete")
    public ResponseEntity<Result<Boolean>> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        return ResponseEntity.ok(Result.ok(deleted));
    }

    // ---------- 条件查询 + 分页 ----------
    @GetMapping("/query")
    @RequiresAuthentication
    public ResponseEntity<Result<Map<String, Object>>> queryUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) Integer gender,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Map<String, Object> criteria = new HashMap<>();
        if (username != null) criteria.put("username", username);
        if (email != null) criteria.put("email", email);
        if (password != null) criteria.put("password", password);
        if (age != null) criteria.put("age", age);
        if (gender != null) criteria.put("gender", gender);

        Map<String, Object> resultMap = userQueryService.queryUsers(criteria, page, pageSize);
        return ResponseEntity.ok(Result.ok(resultMap));
    }

    // ---------- 获取单列去重列表 ----------
    @GetMapping("/options/username")
    public ResponseEntity<Result<List<String>>> getUsernameList() {
        List<String> list = userService.getUsernameList();
        return ResponseEntity.ok(Result.ok(list));
    }

    @GetMapping("/options/email")
    public ResponseEntity<Result<List<String>>> getEmailList() {
        List<String> list = userService.getEmailList();
        return ResponseEntity.ok(Result.ok(list));
    }

    @GetMapping("/options/age")
    public ResponseEntity<Result<List<Integer>>> getAgeList() {
        List<Integer> list = userService.getAgeList();
        return ResponseEntity.ok(Result.ok(list));
    }

    @GetMapping("/options/gender")
    public ResponseEntity<Result<List<Integer>>> getGenderList() {
        List<Integer> list = userService.getGenderList();
        return ResponseEntity.ok(Result.ok(list));
    }
}
