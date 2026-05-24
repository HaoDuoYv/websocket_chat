package com.chat.interceptor;

import com.chat.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractTokenFromUri(request.getURI());
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = jwtUtil.getUserId(claims);
            String username = jwtUtil.getUsername(claims);
            int tokenVersion = jwtUtil.getTokenVersion(claims);
            attributes.put("userId", userId);
            attributes.put("username", username);
            attributes.put("tokenVersion", tokenVersion);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractTokenFromUri(URI uri) {
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && "token".equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }
}
