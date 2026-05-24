package com.chat.service;

import com.chat.entity.FileRecord;
import com.chat.properties.LocalProperties;
import com.chat.repository.FileRecordRepository;
import com.chat.utils.LocalUploadUtil;
import com.chat.utils.SnowflakeIdGenerator;
import com.chat.vo.FileUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Optional;

@Service
@Slf4j
public class FileUploadService {

    @Autowired
    private LocalProperties localProperties;

    @Autowired
    private FileRecordRepository fileRecordRepository;

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Value("${server.port:8081}")
    private int configuredServerPort;

    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, String chatId, String senderId,
                                          String scheme, String serverName, int serverPort) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long fileSize = file.getSize();

        // 计算内容哈希
        byte[] fileBytes = file.getBytes();
        String contentHash = sha256(fileBytes);

        // 检查是否已存在相同内容的文件
        Optional<FileRecord> existing = fileRecordRepository.findByContentHash(contentHash);
        if (existing.isPresent()) {
            FileRecord record = existing.get();
            record.setRefCount(record.getRefCount() + 1);
            fileRecordRepository.save(record);
            log.info("文件已存在（内容去重），引用计数+1: {}", record.getFileUrl());

            FileUploadResponse response = new FileUploadResponse();
            response.setSuccess(true);
            response.setMessage("文件上传成功");
            response.setFileId(record.getFileId());
            response.setFileName(originalFilename);
            response.setFileSize(fileSize);
            response.setFileUrl(record.getFileUrl());
            response.setFileType(contentType);
            return response;
        }

        // 新文件：保存到磁盘
        LocalUploadUtil uploadUtil = new LocalUploadUtil(localProperties);
        String relativeFileUrl = uploadUtil.uploadWithInfo(file, null, chatId, senderId);
        int resolvedPort = resolveFilePort(serverPort);
        String fileUrl = uploadUtil.toAbsoluteFileUrl(relativeFileUrl, scheme, serverName, resolvedPort);

        // 保存文件记录到数据库
        String fileId = String.valueOf(idGenerator.nextId());
        FileRecord record = new FileRecord(fileId, contentHash, originalFilename, fileUrl, fileSize, contentType);
        fileRecordRepository.save(record);

        log.info("文件上传成功（新建）: fileId={}, hash={}", fileId, contentHash);

        FileUploadResponse response = new FileUploadResponse();
        response.setSuccess(true);
        response.setMessage("文件上传成功");
        response.setFileId(fileId);
        response.setFileName(originalFilename);
        response.setFileSize(fileSize);
        response.setFileUrl(fileUrl);
        response.setFileType(contentType);
        return response;
    }

    @Transactional(readOnly = true)
    public FileUploadResponse getFileInfo(String fileId, jakarta.servlet.http.HttpServletRequest request) {
        Optional<FileRecord> opt = fileRecordRepository.findById(fileId);
        if (opt.isEmpty()) {
            FileUploadResponse response = new FileUploadResponse();
            response.setSuccess(false);
            response.setMessage("文件不存在");
            return response;
        }
        FileRecord record = opt.get();
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

    @Transactional
    public void decrementRefCount(String fileId) {
        fileRecordRepository.findById(fileId).ifPresent(record -> {
            record.setRefCount(record.getRefCount() - 1);
            fileRecordRepository.save(record);
        });
    }

    private int resolveFilePort(int requestPort) {
        if (requestPort > 0 && requestPort != 80 && requestPort != 443) {
            return requestPort;
        }
        return configuredServerPort;
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
