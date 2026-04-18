package com.chat.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.*;

@Entity
@Table(name = "room_members")
@IdClass(RoomMemberId.class)
public class RoomMember {
    @Id
    @Column(name = "room_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roomId;

    @Id
    @Column(name = "user_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private Long joinedAt;

    @Column(name = "last_read_seq")
    private Long lastReadSeq = 0L;

    public RoomMember() {
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Long getLastReadSeq() {
        return lastReadSeq;
    }

    public void setLastReadSeq(Long lastReadSeq) {
        this.lastReadSeq = lastReadSeq;
    }
}
