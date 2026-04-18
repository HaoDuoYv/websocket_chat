package com.chat.entity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(length = 128)
    private String password;

    @Column(name = "avatar_color", length = 7)
    private String avatarColor;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "last_seen")
    private Long lastSeen;

    @Column(nullable = false)
    private boolean banned = false;

    @Column(name = "banned_at")
    private Long bannedAt;

    @Column(name = "banned_reason", length = 255)
    private String bannedReason;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAvatarColor() {
        return avatarColor;
    }

    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public Long getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(Long bannedAt) {
        this.bannedAt = bannedAt;
    }

    public String getBannedReason() {
        return bannedReason;
    }

    public void setBannedReason(String bannedReason) {
        this.bannedReason = bannedReason;
    }
}
