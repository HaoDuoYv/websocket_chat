package com.chat.entity;

import java.time.LocalDateTime;

public class FileRecord {
    private String fileId;
    private String fileName;
    private String originalFileName;
    private String filePath;
    private String fileUrl;
    private long fileSize;
    private String fileType;
    private String chatId;
    private String senderId;
    private LocalDateTime uploadTime;

    public FileRecord() {
        this.uploadTime = LocalDateTime.now();
    }

    public FileRecord(String fileId, String fileName, String originalFileName, 
                     String filePath, String fileUrl, long fileSize, 
                     String fileType, String chatId, String senderId) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.filePath = filePath;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.chatId = chatId;
        this.senderId = senderId;
        this.uploadTime = LocalDateTime.now();
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
}
