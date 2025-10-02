package com.egg.springboot_egg.config.shrioandjwt;

import org.apache.shiro.authc.AuthenticationToken;
import java.util.List;

public class JwtToken implements AuthenticationToken {

    //Token类
    private String username;
    private String token;
    private List<String> roles;
    private List<String> permissions;

    public JwtToken(String username, String token, List<String> roles, List<String> permissions) {
        this.username = username;
        this.token = token;
        this.roles = roles;
        this.permissions = permissions;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
