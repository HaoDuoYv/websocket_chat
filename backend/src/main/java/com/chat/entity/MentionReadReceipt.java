package com.chat.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;

@Entity
@Table(name = "mention_read_receipts", indexes = {
    @Index(name = "idx_mention_receipt_mention_id", columnList = "mention_id"),
    @Index(name = "idx_mention_receipt_user_id", columnList = "user_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"mention_id", "user_id"})
})
public class MentionReadReceipt {
    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Column(name = "mention_id", nullable = false)
    private String mentionId;

    @Column(name = "user_id", nullable = false)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Column(name = "read_at", nullable = false)
    private Long readAt;

    public MentionReadReceipt() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMentionId() {
        return mentionId;
    }

    public void setMentionId(String mentionId) {
        this.mentionId = mentionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getReadAt() {
        return readAt;
    }

    public void setReadAt(Long readAt) {
        this.readAt = readAt;
    }
}
