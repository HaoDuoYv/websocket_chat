package com.chat.vo;

public class FileUploadResponse {
    private boolean success;
    private String message;
    private String fileId;
    private String fileName;
    private long fileSize;
    private String fileUrl;
    private String fileType;

    public FileUploadResponse() {
    }

    public FileUploadResponse(boolean success, String message, String fileId, 
                             String fileName, long fileSize, String fileUrl) {
        this.success = success;
        this.message = message;
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileUrl = fileUrl;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
