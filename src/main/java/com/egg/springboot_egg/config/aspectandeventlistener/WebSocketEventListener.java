package com.egg.springboot_egg.config.aspectandeventlistener;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    // 存在线用户ID
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public Set<String> getOnlineUsers() {
        return onlineUsers;
    }

    public boolean isOnline(String userId) {
        return onlineUsers.contains(userId);
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser().getName(); // WebSocketConfig 绑定的 Principal 名称
        onlineUsers.add(userId);
        System.out.println("[WebSocketEventListener] 用户上线: " + userId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser().getName();
        onlineUsers.remove(userId);
        System.out.println("[WebSocketEventListener] 用户下线: " + userId);
    }
}
