package com.egg.springboot_egg.config.websocket;

import com.egg.springboot_egg.config.shrioandjwt.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    String username = accessor.getFirstNativeHeader("username");

                    System.out.println("[WebSocketConfig] CONNECT headers: username=" + username + ", Authorization=" + token);

                    if (token == null || !token.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("缺少或非法 JWT");
                    }
                    token = token.substring(7);

                    try {
                        Claims claims = JwtUtil.parseToken(token);
                        String tokenUsername = claims.getSubject();

                        if (!tokenUsername.equals(username)) {
                            throw new IllegalArgumentException("JWT 与 username 不匹配");
                        }

                        // JWT 校验通过，绑定 Principal
                        accessor.setUser(() -> username);
                        System.out.println("[WebSocketConfig] JWT 验证通过，绑定 Principal username=" + username);

                    } catch (Exception e) {
                        System.out.println("[WebSocketConfig] JWT 无效: " + e.getMessage());
                        throw new IllegalArgumentException("JWT 无效");
                    }
                }

                return message;
            }
        });
    }
}
