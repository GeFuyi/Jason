package com.egg.springboot_egg.service.upclass;

import com.egg.springboot_egg.entity.User;
import com.egg.springboot_egg.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserQueryService {

    @Autowired
    private UserService userService;  // 注入接口，代理生效

    public Map<String, Object> queryUsers(Map<String, Object> criteria, int page, int pageSize) {
        List<User> all = userService.getAllUsers(); // ✅ 会触发缓存

        List<User> filtered = all.stream().filter(u -> {
            boolean match = true;

            Object username = criteria.get("username");
            if (username != null && !"".equals(username.toString().trim())) {
                match = match && u.getUsername().equals(username.toString().trim());
            }

            Object email = criteria.get("email");
            if (email != null && !"".equals(email.toString().trim())) {
                match = match && u.getEmail().equals(email.toString().trim());
            }

            Object password = criteria.get("password");
            if (password != null && !"".equals(password.toString().trim())) {
                match = match && u.getPassword().equals(password.toString().trim());
            }

            Object age = criteria.get("age");
            if (age != null && !"".equals(age.toString().trim())) {
                match = match && u.getAge().equals(Integer.valueOf(age.toString().trim()));
            }

            Object gender = criteria.get("gender");
            if (gender != null && !"".equals(gender.toString().trim())) {
                match = match && u.getGender().equals(Integer.valueOf(gender.toString().trim()));
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
}
