package com.chat.controller;

import com.chat.service.FileUploadService;
import com.chat.vo.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
@CrossOrigin(origins = "*")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("chatId") String chatId,
            @RequestParam("senderId") String senderId,
            jakarta.servlet.http.HttpServletRequest request) {

        logger.info("收到文件上传请求 - chatId: {}, senderId: {}, 文件名: {}",
                chatId, senderId, file.getOriginalFilename());

        // 检查文件是否为空
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new FileUploadResponse(false, "文件不能为空", null, null, 0, null));
        }

        // 检查是否为文件夹（通过判断文件名是否包含路径分隔符或文件类型）
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("/") || originalFilename.contains("\\")) {
            return ResponseEntity.badRequest()
                    .body(new FileUploadResponse(false, "不支持上传文件夹", null, null, 0, null));
        }

        // 检查文件大小（500MB = 524288000 bytes）
        long maxSize = 524288000L;
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest()
                    .body(new FileUploadResponse(false, "文件大小不能超过500MB", null, null, 0, null));
        }

        try {
            FileUploadResponse response = fileUploadService.uploadFile(
                    file,
                    chatId,
                    senderId,
                    resolveScheme(request),
                    resolveServerName(request),
                    resolveServerPort(request));
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("文件上传失败", e);
            return ResponseEntity.internalServerError()
                    .body(new FileUploadResponse(false, "文件上传失败: " + e.getMessage(),
                            null, null, 0, null));
        }
    }

    @GetMapping("/info/{fileId}")
    public ResponseEntity<FileUploadResponse> getFileInfo(@PathVariable String fileId,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            FileUploadResponse response = fileUploadService.getFileInfo(fileId, request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取文件信息失败", e);
            return ResponseEntity.internalServerError()
                    .body(new FileUploadResponse(false, "获取文件信息失败: " + e.getMessage(),
                            null, null, 0, null));
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
