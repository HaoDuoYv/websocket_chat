package com.chat.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    private String id;  // UUID

    @Column(name = "room_id", nullable = false)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roomId;

    @Column(name = "sender_id", nullable = false)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 16)
    private String type;  // 'text' | 'file' | 'system'

    private Long seq;

    @Column(nullable = false)
    private Long timestamp;

    // 文件消息附加字段
    @Column(name = "file_id", length = 64)
    private String fileId;

    @Column(name = "file_name", length = 256)
    private String fileName;

    @Column(name = "file_url", length = 512)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type", length = 32)
    private String fileType;

    public Message() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getSeq() {
        return seq;
    }

    public void setSeq(Long seq) {
        this.seq = seq;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
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

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
