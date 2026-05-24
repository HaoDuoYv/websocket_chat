package com.chat.config;

import com.chat.interceptor.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.chat.handler.ChatWebSocketHandler;
import com.chat.handler.EditorWebSocketHandler;
import com.chat.handler.GomokuWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final GomokuWebSocketHandler gomokuWebSocketHandler;
    private final EditorWebSocketHandler editorWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                          GomokuWebSocketHandler gomokuWebSocketHandler,
                          EditorWebSocketHandler editorWebSocketHandler,
                          WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.gomokuWebSocketHandler = gomokuWebSocketHandler;
        this.editorWebSocketHandler = editorWebSocketHandler;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*");
        registry.addHandler(gomokuWebSocketHandler, "/ws/gomoku")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*");
        registry.addHandler(editorWebSocketHandler, "/ws/editor")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*");
    }
}
