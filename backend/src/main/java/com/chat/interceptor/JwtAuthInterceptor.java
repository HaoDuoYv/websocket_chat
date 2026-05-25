package com.chat.interceptor;

import com.chat.utils.JwtUtil;
import com.chat.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\",\"data\":null}");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = jwtUtil.getUserId(claims);
            String username = jwtUtil.getUsername(claims);
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("tokenVersion", jwtUtil.getTokenVersion(claims));

            // 检查用户是否被封禁
            userService.findById(userId).ifPresent(user -> {
                if (user.isBanned()) {
                    request.setAttribute("banned", true);
                    request.setAttribute("bannedReason", user.getBannedReason());
                }
            });

            if (Boolean.TRUE.equals(request.getAttribute("banned"))) {
                String reason = (String) request.getAttribute("bannedReason");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                String msg = reason != null && !reason.isEmpty()
                        ? "{\"code\":403,\"message\":\"账号已被封禁：" + reason.replace("\"", "\\\"") + "\",\"data\":null}"
                        : "{\"code\":403,\"message\":\"账号已被封禁\",\"data\":null}";
                response.getWriter().write(msg);
                return false;
            }

            return true;
        } catch (JwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null}");
            return false;
        }
    }
}
