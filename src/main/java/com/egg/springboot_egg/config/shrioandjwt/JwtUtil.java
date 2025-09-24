package com.egg.springboot_egg.config.shrioandjwt;

import io.jsonwebtoken.*;
import java.util.Date;
import java.util.List;

public class JwtUtil {

    private static final String SECRET = "eggSecretKeyForJwtAuth2025_VeryStrong!@#1234567890";// 可以放到 application.yml

    //生成token
    public static String generateToken(String username, List<String> roles, List<String> permissions) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 24*60*60*1000)) // 24小时
                .signWith(SignatureAlgorithm.HS256, SECRET.getBytes())
                .compact();
    }
    //解析token
    public static Claims parseToken(String token) throws ExpiredJwtException, UnsupportedJwtException, MalformedJwtException {
        return Jwts.parser().setSigningKey(SECRET.getBytes()).parseClaimsJws(token).getBody();
    }
}
