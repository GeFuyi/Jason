package com.egg.springboot_egg.config.aspectandeventlistener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    // 存在线用户名
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public Set<String> getOnlineUsers() {
        return onlineUsers;
    }

    public boolean isOnline(String username) {
        return onlineUsers.contains(username);
    }

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getUser() != null ? accessor.getUser().getName() : null;
        if (username == null) return;

        onlineUsers.add(username);
        System.out.println("[WebSocketEventListener] 用户上线: " + username);
        System.out.println("[WebSocketEventListener] 当前在线列表: " + onlineUsers);

        // 广播在线用户列表
        messagingTemplate.convertAndSend("/topic/online-users", new ArrayList<>(onlineUsers));
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getUser() != null ? accessor.getUser().getName() : null;
        if (username == null) return;

        onlineUsers.remove(username);
        System.out.println("[WebSocketEventListener] 用户下线: " + username);
        System.out.println("[WebSocketEventListener] 当前在线列表: " + onlineUsers);

        // 广播在线用户列表
        messagingTemplate.convertAndSend("/topic/online-users", new ArrayList<>(onlineUsers));
    }

}
