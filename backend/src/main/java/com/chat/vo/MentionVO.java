package com.chat.vo;

public class MentionVO {
    private String id;
    private String messageId;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String content;
    private boolean isAll;
    private Long timestamp;

    public MentionVO() {
    }

    public MentionVO(String id, String messageId, Long roomId, Long senderId, 
                     String senderName, String content, boolean isAll, Long timestamp) {
        this.id = id;
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.isAll = isAll;
        this.timestamp = timestamp;
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

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isAll() {
        return isAll;
    }

    public void setAll(boolean all) {
        isAll = all;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
