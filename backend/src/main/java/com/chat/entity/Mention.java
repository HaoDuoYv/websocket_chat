package com.chat.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;

@Entity
@Table(name = "mentions", indexes = {
    @Index(name = "idx_mention_message_id", columnList = "message_id"),
    @Index(name = "idx_mention_user_room", columnList = "mentioned_user_id, room_id")
})
public class Mention {
    @Id
    private String id;  // UUID

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "room_id", nullable = false)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roomId;

    @Column(name = "sender_id", nullable = false)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    @Column(name = "mentioned_user_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long mentionedUserId;  // null表示@所有人

    @Column(name = "is_all", nullable = false)
    private boolean isAll = false;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    public Mention() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

    public Long getMentionedUserId() {
        return mentionedUserId;
    }

    public void setMentionedUserId(Long mentionedUserId) {
        this.mentionedUserId = mentionedUserId;
    }

    public boolean isAll() {
        return isAll;
    }

    public void setAll(boolean all) {
        isAll = all;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
