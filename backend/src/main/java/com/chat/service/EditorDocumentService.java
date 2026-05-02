package com.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EditorDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(EditorDocumentService.class);
    private static final long ROOM_IDLE_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes

    private final ConcurrentHashMap<String, EditorRoom> rooms = new ConcurrentHashMap<>();

    public EditorRoom createRoom(String docId, String roomName, String password, String creatorId, String creatorName) {
        EditorRoom room = new EditorRoom(docId, roomName, password, creatorId, creatorName);
        rooms.put(docId, room);
        logger.info("创建编辑器房间: {} ({})", roomName, docId);
        return room;
    }

    public EditorRoom getOrCreateRoom(String docId) {
        return rooms.computeIfAbsent(docId, id -> new EditorRoom(id, "未命名文档", null, null, null));
    }

    public EditorRoom getRoom(String docId) {
        return rooms.get(docId);
    }

    public void removeRoom(String docId) {
        rooms.remove(docId);
        logger.info("删除编辑器房间: {}", docId);
    }

    public boolean checkPassword(String docId, String password) {
        EditorRoom room = rooms.get(docId);
        if (room == null) return false;
        if (room.password == null || room.password.isEmpty()) return true;
        return room.password.equals(password);
    }

    public EditorRoom findUserRoom(String userId) {
        for (EditorRoom room : rooms.values()) {
            if (room.activeUserIds.contains(userId)) {
                return room;
            }
        }
        return null;
    }

    public List<EditorRoom> getRoomList() {
        return new ArrayList<>(rooms.values());
    }

    public void updateActivity(String docId) {
        EditorRoom room = rooms.get(docId);
        if (room != null) {
            room.lastActivity = System.currentTimeMillis();
        }
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupIdleRooms() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, EditorRoom>> it = rooms.entrySet().iterator();
        while (it.hasNext()) {
            EditorRoom room = it.next().getValue();
            if (room.activeUserIds.isEmpty() && (now - room.lastActivity) > ROOM_IDLE_TIMEOUT_MS) {
                it.remove();
                logger.info("清理闲置编辑器文档: {}", room.docId);
            }
        }
    }

    public static class EditorRoom {
        private final String docId;
        private String roomName;
        private String password;
        private final String creatorId;
        private final String creatorName;
        private final Set<String> activeUserIds = ConcurrentHashMap.newKeySet();
        private final long createdAt;
        private long lastActivity;

        public EditorRoom(String docId, String roomName, String password, String creatorId, String creatorName) {
            this.docId = docId;
            this.roomName = roomName;
            this.password = password;
            this.creatorId = creatorId;
            this.creatorName = creatorName;
            this.createdAt = System.currentTimeMillis();
            this.lastActivity = System.currentTimeMillis();
        }

        public String getDocId() { return docId; }
        public String getRoomName() { return roomName; }
        public void setRoomName(String roomName) { this.roomName = roomName; }
        public String getPassword() { return password; }
        public String getCreatorId() { return creatorId; }
        public String getCreatorName() { return creatorName; }
        public Set<String> getActiveUserIds() { return activeUserIds; }
        public long getCreatedAt() { return createdAt; }
        public long getLastActivity() { return lastActivity; }
        public void setLastActivity(long lastActivity) { this.lastActivity = lastActivity; }
    }
}
