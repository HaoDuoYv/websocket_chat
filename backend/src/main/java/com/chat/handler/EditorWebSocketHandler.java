package com.chat.handler;

import com.chat.service.EditorDocumentService;
import com.chat.service.EditorDocumentService.EditorRoom;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EditorWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(EditorWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUsernameMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> docSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionDocMap = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> lobbySessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EditorDocumentService editorDocumentService;

    private Map<String, Object> safeMap(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) {
                map.put((String) keyValues[i], keyValues[i + 1]);
            }
        }
        return map;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("编辑器 WebSocket 连接建立 - Session ID: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Event event = objectMapper.readValue(payload, Event.class);

        switch (event.getType()) {
            case "editor:lobby:join":
                handleLobbyJoin(session, event.getData());
                break;
            case "editor:room:create":
                handleRoomCreate(session, event.getData());
                break;
            case "editor:room:join":
                handleRoomJoin(session, event.getData());
                break;
            case "editor:room:leave":
                handleRoomLeave(session, event.getData());
                break;
            case "editor:join":
                handleDocJoin(session, event.getData());
                break;
            case "editor:leave":
                handleDocLeave(session, event.getData());
                break;
            case "editor:yjs-update":
                handleYjsUpdate(session, event.getData());
                break;
            default:
                logger.warn("编辑器未知事件类型: {}", event.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        logger.info("编辑器 WebSocket 连接关闭 - Session ID: {}", sessionId);

        String userId = sessionUserMap.get(sessionId);
        String username = sessionUsernameMap.get(sessionId);
        String docId = sessionDocMap.get(sessionId);

        sessions.remove(sessionId);
        sessionUserMap.remove(sessionId);
        sessionUsernameMap.remove(sessionId);
        lobbySessions.remove(sessionId);

        if (userId != null) {
            userSessionMap.remove(userId);
        }

        if (docId != null) {
            sessionDocMap.remove(sessionId);
            Set<String> docSessionSet = docSessions.get(docId);
            if (docSessionSet != null) {
                docSessionSet.remove(sessionId);
                if (docSessionSet.isEmpty()) {
                    docSessions.remove(docId);
                }
            }

            EditorRoom room = editorDocumentService.getRoom(docId);
            if (room != null && userId != null) {
                room.getActiveUserIds().remove(userId);
                editorDocumentService.updateActivity(docId);

                try {
                    broadcastToDoc(docId, new Event("editor:participant:left", safeMap(
                            "userId", userId,
                            "username", username != null ? username : userId
                    )));
                } catch (IOException e) {
                    logger.error("广播参与者离开失败: {}", e.getMessage());
                }

                // Remove room if empty
                if (room.getActiveUserIds().isEmpty()) {
                    editorDocumentService.removeRoom(docId);
                }

                broadcastRoomListToLobby();
            }
        }
    }

    // ========== Lobby ==========

    private void handleLobbyJoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long userId = parseLongId(data.get("userId"));
        String username = (String) data.get("username");

        if (userId == null) {
            sendToSession(session.getId(), new Event("editor:error", safeMap("message", "缺少必要参数")));
            return;
        }

        String userIdStr = String.valueOf(userId);
        sessions.put(session.getId(), session);
        userSessionMap.put(userIdStr, session.getId());
        sessionUserMap.put(session.getId(), userIdStr);
        sessionUsernameMap.put(session.getId(), username != null ? username : userIdStr);

        lobbySessions.put(session.getId(), session);

        // Send room list
        sendToSession(session.getId(), new Event("editor:room:list", buildRoomListData()));
    }

    // ========== Room Management ==========

    private void handleRoomCreate(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomName = (String) data.get("roomName");
        String password = (String) data.get("password");
        Long userId = parseLongId(data.get("userId"));
        String username = (String) data.get("username");

        if (roomName == null || userId == null) {
            sendToSession(session.getId(), new Event("editor:error", safeMap("message", "缺少必要参数")));
            return;
        }

        String userIdStr = String.valueOf(userId);
        String docId = generateDocId();

        EditorRoom room = editorDocumentService.createRoom(docId, roomName, password, userIdStr, username);

        // Move from lobby to room
        lobbySessions.remove(session.getId());
        sessionDocMap.put(session.getId(), docId);
        docSessions.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());
        room.getActiveUserIds().add(userIdStr);

        // Register session mappings if not already
        sessions.put(session.getId(), session);
        userSessionMap.put(userIdStr, session.getId());
        sessionUserMap.put(session.getId(), userIdStr);
        sessionUsernameMap.put(session.getId(), username != null ? username : userIdStr);

        // Send created confirmation
        Map<String, Object> createdData = new HashMap<>();
        createdData.put("docId", docId);
        createdData.put("roomName", roomName);
        createdData.put("participants", buildParticipantList(room));
        sendToSession(session.getId(), new Event("editor:room:created", createdData));

        // Broadcast updated room list to lobby
        broadcastRoomListToLobby();
    }

    private void handleRoomJoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        String docId = (String) data.get("docId");
        String password = (String) data.get("password");
        Long userId = parseLongId(data.get("userId"));
        String username = (String) data.get("username");

        if (docId == null || userId == null) {
            sendToSession(session.getId(), new Event("editor:error", safeMap("message", "缺少必要参数")));
            return;
        }

        EditorRoom room = editorDocumentService.getRoom(docId);
        if (room == null) {
            sendToSession(session.getId(), new Event("editor:room:join:failed", safeMap("message", "房间不存在")));
            return;
        }

        if (!editorDocumentService.checkPassword(docId, password)) {
            sendToSession(session.getId(), new Event("editor:room:join:failed", safeMap("message", "密码错误")));
            return;
        }

        String userIdStr = String.valueOf(userId);

        // Move from lobby to room
        lobbySessions.remove(session.getId());
        sessionDocMap.put(session.getId(), docId);
        docSessions.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());
        room.getActiveUserIds().add(userIdStr);
        editorDocumentService.updateActivity(docId);

        // Register session mappings
        sessions.put(session.getId(), session);
        userSessionMap.put(userIdStr, session.getId());
        sessionUserMap.put(session.getId(), userIdStr);
        sessionUsernameMap.put(session.getId(), username != null ? username : userIdStr);

        // Build participant list
        List<Map<String, Object>> participants = buildParticipantList(room);

        // Send join confirmation
        Map<String, Object> joinData = new HashMap<>();
        joinData.put("docId", docId);
        joinData.put("roomName", room.getRoomName());
        joinData.put("participants", participants);
        sendToSession(session.getId(), new Event("editor:room:joined", joinData));

        // Broadcast to others in the doc
        broadcastToDoc(docId, new Event("editor:participant:joined", safeMap(
                "userId", userIdStr,
                "username", username != null ? username : userIdStr
        )), session.getId());

        // Ask existing clients to initiate Yjs sync
        if (room.getActiveUserIds().size() > 1) {
            broadcastToDoc(docId, new Event("editor:sync-request", safeMap(
                    "docId", docId,
                    "forUserId", userIdStr
            )), session.getId());
        }

        // Update room list for lobby
        broadcastRoomListToLobby();
    }

    private void handleRoomLeave(WebSocketSession session, Map<String, Object> data) throws IOException {
        String docId = (String) data.get("docId");
        String userId = sessionUserMap.get(session.getId());
        String username = sessionUsernameMap.get(session.getId());

        if (docId == null || userId == null) return;

        leaveDoc(docId, session.getId(), userId, username);
    }

    // ========== Doc Operations (in-room) ==========

    private void handleDocJoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        String docId = (String) data.get("docId");
        Long userId = parseLongId(data.get("userId"));
        String username = (String) data.get("username");

        if (docId == null || userId == null) {
            sendToSession(session.getId(), new Event("editor:error", safeMap("message", "缺少必要参数")));
            return;
        }

        String userIdStr = String.valueOf(userId);

        // Register session
        sessions.put(session.getId(), session);
        userSessionMap.put(userIdStr, session.getId());
        sessionUserMap.put(session.getId(), userIdStr);
        sessionUsernameMap.put(session.getId(), username != null ? username : userIdStr);

        // Leave previous doc if any
        String prevDocId = sessionDocMap.get(session.getId());
        if (prevDocId != null && !prevDocId.equals(docId)) {
            removeFromDoc(prevDocId, session.getId(), userIdStr);
        }

        sessionDocMap.put(session.getId(), docId);
        docSessions.computeIfAbsent(docId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());

        EditorRoom room = editorDocumentService.getOrCreateRoom(docId);
        room.getActiveUserIds().add(userIdStr);
        editorDocumentService.updateActivity(docId);

        List<Map<String, Object>> participants = buildParticipantList(room);

        Map<String, Object> joinData = new HashMap<>();
        joinData.put("docId", docId);
        joinData.put("roomName", room.getRoomName());
        joinData.put("participants", participants);
        sendToSession(session.getId(), new Event("editor:joined", joinData));

        broadcastToDoc(docId, new Event("editor:participant:joined", safeMap(
                "userId", userIdStr,
                "username", username != null ? username : userIdStr
        )), session.getId());

        if (room.getActiveUserIds().size() > 1) {
            broadcastToDoc(docId, new Event("editor:sync-request", safeMap(
                    "docId", docId,
                    "forUserId", userIdStr
            )), session.getId());
        }
    }

    private void handleDocLeave(WebSocketSession session, Map<String, Object> data) throws IOException {
        String docId = (String) data.get("docId");
        String userId = sessionUserMap.get(session.getId());
        String username = sessionUsernameMap.get(session.getId());

        if (docId == null || userId == null) return;

        leaveDoc(docId, session.getId(), userId, username);
    }

    private void leaveDoc(String docId, String sessionId, String userId, String username) throws IOException {
        removeFromDoc(docId, sessionId, userId);

        broadcastToDoc(docId, new Event("editor:participant:left", safeMap(
                "userId", userId,
                "username", username != null ? username : userId
        )));

        // Remove room if empty
        EditorRoom room = editorDocumentService.getRoom(docId);
        if (room != null && room.getActiveUserIds().isEmpty()) {
            editorDocumentService.removeRoom(docId);
        }

        // Move back to lobby
        lobbySessions.put(sessionId, sessions.get(sessionId));
        broadcastRoomListToLobby();
    }

    private void handleYjsUpdate(WebSocketSession session, Map<String, Object> data) throws IOException {
        String docId = (String) data.get("docId");
        String updateBase64 = (String) data.get("update");
        String msgType = (String) data.get("msgType");

        if (docId == null || updateBase64 == null) return;

        EditorRoom room = editorDocumentService.getRoom(docId);
        if (room == null) return;

        editorDocumentService.updateActivity(docId);

        broadcastToDoc(docId, new Event("editor:yjs-update", safeMap(
                "docId", docId,
                "update", updateBase64,
                "msgType", msgType
        )), session.getId());
    }

    // ========== Helpers ==========

    private void removeFromDoc(String docId, String sessionId, String userId) {
        Set<String> docSessionSet = docSessions.get(docId);
        if (docSessionSet != null) {
            docSessionSet.remove(sessionId);
            if (docSessionSet.isEmpty()) {
                docSessions.remove(docId);
            }
        }
        sessionDocMap.remove(sessionId);

        EditorRoom room = editorDocumentService.getRoom(docId);
        if (room != null) {
            room.getActiveUserIds().remove(userId);
            editorDocumentService.updateActivity(docId);
        }
    }

    private List<Map<String, Object>> buildParticipantList(EditorRoom room) {
        List<Map<String, Object>> participants = new ArrayList<>();
        for (String uid : room.getActiveUserIds()) {
            String sid = userSessionMap.get(uid);
            String uname = sid != null ? sessionUsernameMap.get(sid) : uid;
            participants.add(safeMap("userId", uid, "username", uname != null ? uname : uid));
        }
        return participants;
    }

    private Map<String, Object> buildRoomListData() {
        List<Map<String, Object>> roomList = new ArrayList<>();
        for (EditorRoom room : editorDocumentService.getRoomList()) {
            Map<String, Object> item = new HashMap<>();
            item.put("docId", room.getDocId());
            item.put("roomName", room.getRoomName());
            item.put("hasPassword", room.getPassword() != null && !room.getPassword().isEmpty());
            item.put("participantCount", room.getActiveUserIds().size());
            item.put("creatorName", room.getCreatorName());
            item.put("createdAt", room.getCreatedAt());
            roomList.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("rooms", roomList);
        return result;
    }

    private void broadcastRoomListToLobby() {
        try {
            Event event = new Event("editor:room:list", buildRoomListData());
            String message = objectMapper.writeValueAsString(event);
            for (Map.Entry<String, WebSocketSession> entry : lobbySessions.entrySet()) {
                WebSocketSession session = entry.getValue();
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        logger.warn("广播房间列表失败: {}", e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("序列化房间列表失败: {}", e.getMessage());
        }
    }

    private String generateDocId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private Long parseLongId(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        throw new IllegalArgumentException("Invalid ID value: " + value);
    }

    private void sendToSession(String sessionId, Event event) {
        try {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                String message = objectMapper.writeValueAsString(event);
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            logger.error("发送消息失败: {}", e.getMessage());
        }
    }

    private void broadcastToDoc(String docId, Event event) throws IOException {
        broadcastToDoc(docId, event, null);
    }

    private void broadcastToDoc(String docId, Event event, String excludeSessionId) throws IOException {
        Set<String> sessIds = docSessions.get(docId);
        if (sessIds == null || sessIds.isEmpty()) return;

        String message = objectMapper.writeValueAsString(event);
        for (String sessId : sessIds) {
            if (sessId.equals(excludeSessionId)) continue;
            WebSocketSession session = sessions.get(sessId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    logger.warn("广播消息失败: {}", e.getMessage());
                }
            }
        }
    }

    static class Event {
        private String type;
        private Map<String, Object> data;

        public Event() {}

        public Event(String type, Map<String, Object> data) {
            this.type = type;
            this.data = data;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}
