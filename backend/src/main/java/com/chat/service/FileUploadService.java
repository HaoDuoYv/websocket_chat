package com.chat.service;

import com.chat.entity.FileRecord;
import com.chat.properties.LocalProperties;
import com.chat.utils.LocalUploadUtil;
import com.chat.vo.FileUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件上传服务类
 * 使用 LocalUploadUtil 工具类处理文件上传
 * 参考苍穹外卖项目实现
 */
@Service
@Slf4j
public class FileUploadService {

    @Autowired
    private LocalProperties localProperties;

    @Value("${server.port:8081}")
    private int configuredServerPort;

    // 内存中存储文件记录（实际项目中应该使用数据库）
    private final Map<String, FileRecord> fileRecords = new ConcurrentHashMap<>();

    /**
     * 上传文件
     *
     * @param file     上传的文件
     * @param chatId   聊天ID
     * @param senderId 发送者ID
     * @return 文件上传响应
     * @throws IOException IO异常
     */
    public FileUploadResponse uploadFile(MultipartFile file, String chatId, String senderId, String scheme, String serverName, int serverPort) throws IOException {
        // 生成文件ID
        String fileId = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();

        // 使用 LocalUploadUtil 上传文件
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);
        String relativeFileUrl = uploadUtil.uploadWithInfo(file, null, chatId, senderId);
        int resolvedPort = resolveFilePort(serverPort);
        String fileUrl = uploadUtil.toAbsoluteFileUrl(relativeFileUrl, scheme, serverName, resolvedPort);

        // 创建文件记录
        FileRecord fileRecord = new FileRecord(
                fileId,
                file.getOriginalFilename(),
                originalFilename,
                null, // 不再存储绝对路径
                fileUrl,
                file.getSize(),
                file.getContentType(),
                chatId,
                senderId);

        // 保存记录
        fileRecords.put(fileId, fileRecord);

        log.info("文件上传成功 - fileId: {}, 文件名: {}, 大小: {} bytes, URL: {}",
                fileId, originalFilename, file.getSize(), fileUrl);

        // 返回响应
        FileUploadResponse response = new FileUploadResponse();
        response.setSuccess(true);
        response.setMessage("文件上传成功");
        response.setFileId(fileId);
        response.setFileName(originalFilename);
        response.setFileSize(file.getSize());
        response.setFileUrl(fileUrl);
        response.setFileType(file.getContentType());

        return response;
    }

    /**
     * 获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件上传响应
     */
    public FileUploadResponse getFileInfo(String fileId, jakarta.servlet.http.HttpServletRequest request) {
        FileRecord record = fileRecords.get(fileId);
        if (record == null) {
            FileUploadResponse response = new FileUploadResponse();
            response.setSuccess(false);
            response.setMessage("文件不存在");
            return response;
        }

        FileUploadResponse response = new FileUploadResponse();
        response.setSuccess(true);
        response.setMessage("获取成功");
        response.setFileId(record.getFileId());
        response.setFileName(record.getOriginalFileName());
        response.setFileSize(record.getFileSize());
        response.setFileUrl(record.getFileUrl());
        response.setFileType(record.getFileType());

        return response;
    }

    /**
     * 获取文件记录
     *
     * @param fileId 文件ID
     * @return 文件记录
     */
    public FileRecord getFileRecord(String fileId) {
        return fileRecords.get(fileId);
    }

    private int resolveFilePort(int requestPort) {
        if (requestPort > 0 && requestPort != 80 && requestPort != 443) {
            return requestPort;
        }
        return configuredServerPort;
    }
}
