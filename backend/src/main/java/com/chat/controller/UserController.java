package com.chat.controller;

import com.chat.handler.ChatWebSocketHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public UserController(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @GetMapping("/users")
    public Map<String, Object> getOnlineUsers() {
        return Map.of("users", chatWebSocketHandler.getOnlineUsers());
    }
}
