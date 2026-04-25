package com.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.chat.handler.ChatWebSocketHandler;
import com.chat.handler.GomokuWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final GomokuWebSocketHandler gomokuWebSocketHandler;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                          GomokuWebSocketHandler gomokuWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.gomokuWebSocketHandler = gomokuWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat").setAllowedOrigins("*");
        registry.addHandler(gomokuWebSocketHandler, "/ws/gomoku").setAllowedOrigins("*");
    }
}
