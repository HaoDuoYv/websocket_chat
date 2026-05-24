package com.chat.controller;

import com.chat.entity.User;
import com.chat.service.UserService;
import com.chat.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名不能为空"));
        }
        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "密码不能少于6位"));
        }

        try {
            User user = userService.register(username.trim(), password);
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), 0);
            Map<String, Object> response = new HashMap<>();
            response.put("userId", String.valueOf(user.getId()));
            response.put("username", user.getUsername());
            response.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
            response.put("createdAt", user.getCreatedAt());
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名不能为空"));
        }
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请输入密码"));
        }

        try {
            User user = userService.login(username.trim(), password);
            int tokenVersion = userService.incrementTokenVersion(user.getId());
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), tokenVersion);
            Map<String, Object> response = new HashMap<>();
            response.put("userId", String.valueOf(user.getId()));
            response.put("username", user.getUsername());
            response.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
            response.put("createdAt", user.getCreatedAt());
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
