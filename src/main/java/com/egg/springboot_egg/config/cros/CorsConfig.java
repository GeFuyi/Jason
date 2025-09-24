package com.egg.springboot_egg.config.cros;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许携带 Cookie
                .allowCredentials(true)
                // 指定允许访问的前端地址（必须指定具体域名，不能用 "*"）
                .allowedOriginPatterns("http://localhost:8080")
//                .allowedOriginPatterns("*")
                // 允许的请求方法
//                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许的请求头
                .allowedHeaders("*")
                // 前端可读取的响应头
                .exposedHeaders("*");
    }
}
