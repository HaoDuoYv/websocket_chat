package com.chat.controller;

import com.chat.handler.ChatWebSocketHandler;
import com.chat.service.AdminAuthService;
import com.chat.service.LogMonitorService;
import com.chat.service.SystemMonitorService;
import com.chat.service.UserService;
import com.chat.vo.LogLineVO;
import com.chat.vo.SystemMetricsVO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private SystemMonitorService systemMonitorService;

    @Autowired
    private LogMonitorService logMonitorService;

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private UserService userService;

    /**
     * 健康检查（无需IP白名单）
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request, HttpSession session) {
        String username = request.get("username");
        String password = request.get("password");
        boolean success = adminAuthService.login(username, password, session);
        if (!success) {
            return ResponseEntity.status(401).body(Map.of("message", "管理员账号或密码错误"));
        }
        return ResponseEntity.ok(Map.of("message", "登录成功", "username", username));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        adminAuthService.logout(session);
        return ResponseEntity.ok(Map.of("message", "退出成功"));
    }

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> getSession(HttpSession session) {
        return ResponseEntity.ok(adminAuthService.getSessionInfo(session));
    }

    /**
     * 获取系统监控数据
     */
    @GetMapping("/metrics")
    public ResponseEntity<SystemMetricsVO> getMetrics() {
        SystemMetricsVO metrics = systemMonitorService.getSystemMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/online-users")
    public ResponseEntity<Map<String, Object>> getOnlineUsers() {
        return ResponseEntity.ok(Map.of("users", chatWebSocketHandler.getOnlineUsers()));
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        return ResponseEntity.ok(userService.getAdminUserList());
    }

    @PutMapping("/users/{userId}/username")
    public ResponseEntity<Map<String, Object>> renameUser(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        String username = request.get("username");
        userService.renameUser(userId, username);
        return ResponseEntity.ok(Map.of("message", "修改成功"));
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<Map<String, Object>> banUser(@PathVariable Long userId, @RequestBody(required = false) Map<String, String> request) {
        String reason = request == null ? null : request.get("reason");
        userService.banUser(userId, reason);
        chatWebSocketHandler.banOnlineUser(userId, reason);
        return ResponseEntity.ok(Map.of("message", "封禁成功"));
    }

    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<Map<String, Object>> unbanUser(@PathVariable Long userId) {
        userService.unbanUser(userId);
        return ResponseEntity.ok(Map.of("message", "解封成功"));
    }

    /**
     * 获取最近日志
     */
    @GetMapping("/logs")
    public ResponseEntity<List<LogLineVO>> getLogs(
            @RequestParam(defaultValue = "100") int limit) {
        List<LogLineVO> logs = logMonitorService.getRecentLogs(limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * 获取所有日志
     */
    @GetMapping("/logs/all")
    public ResponseEntity<List<LogLineVO>> getAllLogs() {
        List<LogLineVO> logs = logMonitorService.getAllLogs();
        return ResponseEntity.ok(logs);
    }

    /**
     * 清空日志缓存
     */
    @PostMapping("/logs/clear")
    public ResponseEntity<Map<String, Object>> clearLogs() {
        logMonitorService.clearLogs();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "日志缓存已清空");
        return ResponseEntity.ok(result);
    }
}
