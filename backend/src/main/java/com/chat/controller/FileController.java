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
            @RequestParam("senderId") String senderId) {
        
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
            FileUploadResponse response = fileUploadService.uploadFile(file, chatId, senderId);
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
    public ResponseEntity<FileUploadResponse> getFileInfo(@PathVariable String fileId) {
        try {
            FileUploadResponse response = fileUploadService.getFileInfo(fileId);
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
}
