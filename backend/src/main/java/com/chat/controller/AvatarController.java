package com.chat.controller;

import com.chat.service.AvatarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/avatar")
@CrossOrigin(origins = "*")
@Slf4j
public class AvatarController {

    @Autowired
    private AvatarService avatarService;

    /**
     * 上传用户头像
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> uploadUserAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String url = avatarService.uploadUserAvatar(
                    userId, file,
                    resolveScheme(request),
                    resolveServerName(request),
                    resolveServerPort(request));
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

    /**
     * 上传AI助手头像
     */
    @PostMapping("/ai/{assistantId}")
    public ResponseEntity<Map<String, Object>> uploadAiAvatar(
            @PathVariable Long assistantId,
            @RequestParam("file") MultipartFile file,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            String url = avatarService.uploadAiAvatar(
                    assistantId, file,
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
                ? forwardedHost
                : request.getHeader("Host");
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
                ? forwardedHost
                : request.getHeader("Host");
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
