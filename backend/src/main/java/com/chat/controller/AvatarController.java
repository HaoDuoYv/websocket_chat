package com.chat.controller;

import com.chat.entity.AiAssistant;
import com.chat.entity.Room;
import com.chat.repository.AiAssistantRepository;
import com.chat.repository.RoomRepository;
import com.chat.service.AdminAuthService;
import com.chat.service.AvatarService;
import com.chat.handler.ChatWebSocketHandler;
import com.chat.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/avatar")
@CrossOrigin(origins = "*")
@Slf4j
public class AvatarController {

    @Autowired
    private AvatarService avatarService;

    @Autowired
    private AiAssistantRepository aiAssistantRepository;

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    private ResponseEntity<Map<String, Object>> checkSystemAssistantPermission(Long assistantId, HttpSession session) {
        Optional<AiAssistant> opt = aiAssistantRepository.findById(assistantId);
        if (opt.isPresent() && Boolean.TRUE.equals(opt.get().getIsSystem())) {
            if (!adminAuthService.isLoggedIn(session)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "系统助手头像只有管理员可以修改"
                ));
            }
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> checkRoomOwnerPermission(Long roomId, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "未登录"));
        }
        Long userId;
        try {
            Claims claims = jwtUtil.parseToken(authHeader.substring(7));
            userId = jwtUtil.getUserId(claims);
        } catch (JwtException e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "token无效"));
        }
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "房间不存在"));
        }
        Long ownerId = roomOpt.get().getOwnerId();
        if (ownerId == null || !userId.equals(ownerId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "只有群主可以修改群头像"));
        }
        return null;
    }

    /**
     * 第一阶段：上传用户头像到临时目录
     */
    @PostMapping("/user/{userId}/temp")
    public ResponseEntity<Map<String, Object>> uploadUserAvatarTemp(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        try {
            String tempPath = avatarService.uploadUserAvatarTemp(userId, file);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "头像已上传到临时目录",
                    "tempPath", tempPath
            ));
        } catch (Exception e) {
            log.error("用户头像临时上传失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 第二阶段：确认用户头像（从 temp 移到正式目录）
     */
    @PostMapping("/user/{userId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmUserAvatar(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            String tempPath = request.get("tempPath");
            if (tempPath == null || tempPath.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "tempPath 不能为空"
                ));
            }
            String url = avatarService.confirmUserAvatar(
                    userId, tempPath,
                    resolveScheme(httpRequest),
                    resolveServerName(httpRequest),
                    resolveServerPort(httpRequest));
            chatWebSocketHandler.broadcastUserAvatarUpdated(userId, url);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "头像确认成功",
                    "url", url
            ));
        } catch (Exception e) {
            log.error("用户头像确认失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 取消头像上传（删除临时文件）
     */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelAvatar(
            @RequestBody Map<String, String> request) {
        String tempPath = request.get("tempPath");
        if (tempPath != null && !tempPath.isBlank()) {
            avatarService.cancelUserAvatar(tempPath);
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已取消"
        ));
    }

    /**
     * 第一阶段：上传群聊头像到临时目录
     */
    @PostMapping("/room/{roomId}/temp")
    public ResponseEntity<Map<String, Object>> uploadRoomAvatarTemp(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        ResponseEntity<Map<String, Object>> permCheck = checkRoomOwnerPermission(roomId, request);
        if (permCheck != null) return permCheck;
        try {
            String tempPath = avatarService.uploadRoomAvatarTemp(roomId, file);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "头像已上传到临时目录",
                    "tempPath", tempPath
            ));
        } catch (Exception e) {
            log.error("群聊头像临时上传失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 第二阶段：确认群聊头像
     */
    @PostMapping("/room/{roomId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmRoomAvatar(
            @PathVariable Long roomId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        ResponseEntity<Map<String, Object>> permCheck = checkRoomOwnerPermission(roomId, httpRequest);
        if (permCheck != null) return permCheck;
        try {
            String tempPath = request.get("tempPath");
            if (tempPath == null || tempPath.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "tempPath 不能为空"
                ));
            }
            String url = avatarService.confirmRoomAvatar(
                    roomId, tempPath,
                    resolveScheme(httpRequest),
                    resolveServerName(httpRequest),
                    resolveServerPort(httpRequest));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "头像确认成功",
                    "url", url
            ));
        } catch (Exception e) {
            log.error("群聊头像确认失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 第一阶段：上传AI助手头像到临时目录
     */
    @PostMapping("/ai/{assistantId}/temp")
    public ResponseEntity<Map<String, Object>> uploadAiAvatarTemp(
            @PathVariable Long assistantId,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        ResponseEntity<Map<String, Object>> permCheck = checkSystemAssistantPermission(assistantId, session);
        if (permCheck != null) return permCheck;
        try {
            String tempPath = avatarService.uploadAiAvatarTemp(assistantId, file);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "头像已上传到临时目录",
                    "tempPath", tempPath
            ));
        } catch (Exception e) {
            log.error("AI助手头像临时上传失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 第二阶段：确认AI助手头像
     */
    @PostMapping("/ai/{assistantId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmAiAvatar(
            @PathVariable Long assistantId,
            @RequestBody Map<String, String> request,
            jakarta.servlet.http.HttpServletRequest httpRequest,
            HttpSession session) {
        ResponseEntity<Map<String, Object>> permCheck = checkSystemAssistantPermission(assistantId, session);
        if (permCheck != null) return permCheck;
        try {
            String tempPath = request.get("tempPath");
            if (tempPath == null || tempPath.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "tempPath 不能为空"
                ));
            }
            String url = avatarService.confirmAiAvatar(
                    assistantId, tempPath,
                    resolveScheme(httpRequest),
                    resolveServerName(httpRequest),
                    resolveServerPort(httpRequest));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "头像确认成功",
                    "url", url
            ));
        } catch (Exception e) {
            log.error("AI助手头像确认失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 保留旧接口兼容
    @PostMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> uploadUserAvatarLegacy(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String tempPath = avatarService.uploadUserAvatarTemp(userId, file);
            String url = avatarService.confirmUserAvatar(
                    userId, tempPath,
                    resolveScheme(request),
                    resolveServerName(request),
                    resolveServerPort(request));
            chatWebSocketHandler.broadcastUserAvatarUpdated(userId, url);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "头像上传成功",
                    "url", url
            ));
        } catch (Exception e) {
            log.error("用户头像上传失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/ai/{assistantId}")
    public ResponseEntity<Map<String, Object>> uploadAiAvatarLegacy(
            @PathVariable Long assistantId,
            @RequestParam("file") MultipartFile file,
            jakarta.servlet.http.HttpServletRequest request,
            HttpSession session) {
        ResponseEntity<Map<String, Object>> permCheck = checkSystemAssistantPermission(assistantId, session);
        if (permCheck != null) return permCheck;
        try {
            String tempPath = avatarService.uploadAiAvatarTemp(assistantId, file);
            String url = avatarService.confirmAiAvatar(
                    assistantId, tempPath,
                    resolveScheme(request),
                    resolveServerName(request),
                    resolveServerPort(request));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "头像上传成功",
                    "url", url
            ));
        } catch (Exception e) {
            log.error("AI助手头像上传失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    private String resolveScheme(jakarta.servlet.http.HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            return forwardedProto.split(",", 2)[0].trim();
        }
        return request.getScheme();
    }

    private String resolveServerName(jakarta.servlet.http.HttpServletRequest request) {
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String hostHeader = forwardedHost != null && !forwardedHost.isBlank()
                ? forwardedHost : request.getHeader("Host");
        if (hostHeader == null || hostHeader.isBlank()) {
            return request.getServerName();
        }
        String normalizedHost = hostHeader.split(",", 2)[0].trim();
        if (normalizedHost.startsWith("[")) {
            int closingBracketIndex = normalizedHost.indexOf(']');
            if (closingBracketIndex > 0) {
                return normalizedHost.substring(1, closingBracketIndex);
            }
            return request.getServerName();
        }
        String[] hostParts = normalizedHost.split(":", 2);
        return hostParts[0];
    }

    private int resolveServerPort(jakarta.servlet.http.HttpServletRequest request) {
        String forwardedPort = request.getHeader("X-Forwarded-Port");
        if (forwardedPort != null && !forwardedPort.isBlank()) {
            return Integer.parseInt(forwardedPort.split(",", 2)[0].trim());
        }
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String hostHeader = forwardedHost != null && !forwardedHost.isBlank()
                ? forwardedHost : request.getHeader("Host");
        if (hostHeader != null && !hostHeader.isBlank()) {
            String normalizedHost = hostHeader.split(",", 2)[0].trim();
            if (normalizedHost.startsWith("[")) {
                int closingBracketIndex = normalizedHost.indexOf(']');
                if (closingBracketIndex > 0
                        && normalizedHost.length() > closingBracketIndex + 2
                        && normalizedHost.charAt(closingBracketIndex + 1) == ':') {
                    return Integer.parseInt(normalizedHost.substring(closingBracketIndex + 2));
                }
            } else {
                String[] hostParts = normalizedHost.split(":", 2);
                if (hostParts.length == 2 && !hostParts[1].isBlank()) {
                    return Integer.parseInt(hostParts[1]);
                }
            }
        }
        return request.getServerPort();
    }
}
