package com.chat.controller;

import com.chat.entity.User;
import com.chat.service.UserService;
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名不能为空"));
        }

        try {
            User user = userService.register(username.trim());
            Map<String, Object> response = new HashMap<>();
            // 将 Long 类型的 ID 转为 String，避免前端 JavaScript 精度丢失
            response.put("userId", String.valueOf(user.getId()));
            response.put("username", user.getUsername());
            response.put("createdAt", user.getCreatedAt());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名不能为空"));
        }

        try {
            User user = userService.login(username.trim());
            Map<String, Object> response = new HashMap<>();
            // 将 Long 类型的 ID 转为 String，避免前端 JavaScript 精度丢失
            response.put("userId", String.valueOf(user.getId()));
            response.put("username", user.getUsername());
            response.put("createdAt", user.getCreatedAt());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
