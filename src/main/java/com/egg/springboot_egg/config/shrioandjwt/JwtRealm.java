package com.egg.springboot_egg.config.shrioandjwt;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.egg.springboot_egg.GlobalExceptionHandler.BusinessException;
import com.egg.springboot_egg.entity.User;
import com.egg.springboot_egg.mapper.UserMapper;
import com.egg.springboot_egg.service.impl.UserServiceImpl;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtRealm extends AuthorizingRealm {

    //继承Shiro，解析token

    @Autowired
    private UserServiceImpl userService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    // 权限认证
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = (String) principals.getPrimaryPrincipal();
        User user = userService.findByUsernameOrEmail(username);

        // 添加打印：确认授权时用户和角色
        System.out.println("[JwtRealm] Authorization for username: " + username);
        System.out.println("[JwtRealm] User found: " + (user != null ? user.getUsername() : "null"));

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        if (user != null) {
            if (user.getRoles() != null) {
                info.addRoles(user.getRoles());
                System.out.println("[JwtRealm] Added roles: " + user.getRoles());  // 打印添加的角色
            }
            if (user.getPermissions() != null) {
                info.addStringPermissions(user.getPermissions());
                System.out.println("[JwtRealm] Added permissions: " + user.getPermissions());  // 打印添加的权限
            }
        }
        return info;
    }

    // 登录认证（这里只检查 token 是否有效即可）
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        JwtToken jwtToken = (JwtToken) token;
        String username = jwtToken.getPrincipal().toString();

        User user = userService.findByUsernameOrEmail(username);

        if (user == null) {
            throw new UnknownAccountException("用户不存在");
        }

        return new SimpleAuthenticationInfo(username, jwtToken.getCredentials(), getName());
    }

}