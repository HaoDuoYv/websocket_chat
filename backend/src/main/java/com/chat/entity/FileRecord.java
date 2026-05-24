package com.chat.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;

@Entity
@Table(name = "file_records")
public class FileRecord {

    @Id
    @Column(length = 64)
    private String fileId;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Column(name = "file_url", length = 512)
    private String fileUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", length = 128)
    private String fileType;

    @Column(name = "ref_count", nullable = false)
    private int refCount = 1;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    public FileRecord() {
        this.createdAt = System.currentTimeMillis();
    }

    public FileRecord(String fileId, String contentHash, String originalFileName,
                      String fileUrl, Long fileSize, String fileType) {
        this.fileId = fileId;
        this.contentHash = contentHash;
        this.originalFileName = originalFileName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.refCount = 1;
        this.createdAt = System.currentTimeMillis();
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public int getRefCount() { return refCount; }
    public void setRefCount(int refCount) { this.refCount = refCount; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
