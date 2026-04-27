package com.chat.handler;

import com.chat.entity.Message;
import com.chat.entity.Room;
import com.chat.entity.User;
import com.chat.properties.LocalProperties;
import com.chat.service.MessageService;
import com.chat.service.RoomService;
import com.chat.service.UserService;
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
import java.util.stream.Collectors;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    // WebSocket 会话仍然保存在内存中（连接是有状态的）
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, String> userSessionMap = new ConcurrentHashMap<>(); // userId -> sessionId
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>(); // sessionId -> userId
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private LocalProperties localProperties;

    /**
     * 解析 Long 类型的 ID，支持 Number 和 String 类型
     * 用于解决前端 JavaScript 精度丢失问题，前端会将 Long 作为字符串传递
     */
    private Long parseLongId(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        throw new IllegalArgumentException(
                "Invalid ID value: " + value + " (type: " + value.getClass().getName() + ")");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String clientIp = getClientIp(session);
        logger.info("WebSocket 连接建立 - Session ID: {}, IP 地址: {}", session.getId(), clientIp);
    }

    private String getClientIp(WebSocketSession session) {
        Map<String, Object> attributes = session.getAttributes();
        String ip = (String) attributes.get("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = (String) attributes.get("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = session.getRemoteAddress() != null
                    ? session.getRemoteAddress().getAddress().getHostAddress()
                    : "未知";
        }
        return ip;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Event event = objectMapper.readValue(payload, Event.class);

        switch (event.getType()) {
            case "user:join":
                handleUserJoin(session, event.getData());
                break;
            case "user:list":
                handleUserList(session);
                break;
            case "message:send":
                handleMessageSend(session, event.getData());
                break;
            case "message:send:file":
                handleFileMessageSend(session, event.getData());
                break;
            case "message:history":
                handleMessageHistory(session, event.getData());
                break;
            case "room:create":
                handleRoomCreate(session, event.getData());
                break;
            case "room:join":
                handleRoomJoin(session, event.getData());
                break;
            case "room:leave":
                handleRoomLeave(session, event.getData());
                break;
            case "room:list":
                handleRoomList(session, event.getData());
                break;
            case "room:private:start":
                handlePrivateRoomStart(session, event.getData());
                break;
            case "room:sync":
                handleRoomSync(session, event.getData());
                break;
            case "room:invite:member":
                handleRoomInviteMember(session, event.getData());
                break;
            case "room:invite:error":
                break;
            case "message:read":
                handleMessageRead(session, event.getData());
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String clientIp = getClientIp(session);
        logger.info("WebSocket 连接关闭 - Session ID: {}, IP 地址: {}, 状态: {}",
                sessionId, clientIp, status);

        Long userId = sessionUserMap.get(sessionId);

        // 从内存映射中移除
        sessions.remove(sessionId);
        if (userId != null) {
            userSessionMap.remove(userId);
            sessionUserMap.remove(sessionId);

            // 通知其他用户有人离开
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Event leftEvent = new Event("user:left", buildOnlineUser(user.getId(), user.getUsername()));
                broadcastToAll(leftEvent);
            }
        }
    }

    private void handleUserJoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long userId = parseLongId(data.get("userId"));
        String username = (String) data.get("username");
        String clientIp = getClientIp(session);

        logger.info("用户加入 - 用户名: {}, userId: {}, IP 地址: {}, Session ID: {}",
                username, userId, clientIp, session.getId());

        // 保存会话映射
        sessions.put(session.getId(), session);
        userSessionMap.put(userId, session.getId());
        sessionUserMap.put(session.getId(), userId);

        // 更新用户最后活动时间
        userService.updateLastSeen(userId);

        Event joinedEvent = new Event("user:joined", buildOnlineUser(userId, username));
        broadcastToAll(joinedEvent, session.getId());
    }

    private void handleMessageSend(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long roomId = parseLongId(data.get("roomId"));
        String content = (String) data.get("content");
        Long senderId = sessionUserMap.get(session.getId());

        if (senderId == null) {
            logger.warn("发送消息失败：未找到发送者 sessionId={}", session.getId());
            return;
        }

        // 保存消息到数据库
        Message savedMessage = messageService.sendMessage(roomId, senderId, content, "text");
        String senderName = messageService.getSenderName(senderId);

        // 发送消息给房间所有成员 - 将 ID 转为 String 避免前端精度丢失
        Event receiveEvent = new Event("message:new", Map.of(
                "id", String.valueOf(savedMessage.getId()),
                "roomId", String.valueOf(roomId),
                "senderId", String.valueOf(senderId),
                "senderName", senderName,
                "content", content,
                "type", "text",
                "seq", savedMessage.getSeq(),
                "timestamp", savedMessage.getTimestamp()));

        broadcastToRoomMembers(roomId, receiveEvent);
    }

    private void handleFileMessageSend(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long roomId = parseLongId(data.get("roomId"));
        String fileId = (String) data.get("fileId");
        String fileName = (String) data.get("fileName");
        String fileUrl = (String) data.get("fileUrl");
        long fileSize = ((Number) data.get("fileSize")).longValue();
        String fileType = (String) data.get("fileType");
        Long senderId = sessionUserMap.get(session.getId());

        if (senderId == null) {
            logger.warn("发送文件消息失败：未找到发送者 sessionId={}", session.getId());
            return;
        }

        String content = "[文件] " + fileName;

        // 保存文件消息到数据库
        Message savedMessage = messageService.sendFileMessage(
                roomId, senderId, content, fileId, fileName, fileUrl, fileSize, fileType);
        String senderName = messageService.getSenderName(senderId);

        // 发送文件消息给房间所有成员 - 将 ID 转为 String 避免前端精度丢失
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("id", String.valueOf(savedMessage.getId()));
        eventData.put("roomId", String.valueOf(roomId));
        eventData.put("senderId", String.valueOf(senderId));
        eventData.put("senderName", senderName);
        eventData.put("content", content);
        eventData.put("type", "file");
        eventData.put("seq", savedMessage.getSeq());
        eventData.put("timestamp", savedMessage.getTimestamp());
        eventData.put("fileId", fileId);
        eventData.put("fileName", fileName);
        eventData.put("fileUrl", fileUrl);
        eventData.put("fileSize", fileSize);
        eventData.put("fileType", fileType);

        Event receiveEvent = new Event("message:new:file", eventData);
        broadcastToRoomMembers(roomId, receiveEvent);
    }

    private void handleRoomCreate(WebSocketSession session, Map<String, Object> data) throws IOException {
        String name = (String) data.get("name");
        Long ownerId = sessionUserMap.get(session.getId());
        @SuppressWarnings("unchecked")
        List<String> participants = (List<String>) data.get("participants");

        if (ownerId == null) {
            logger.warn("创建房间失败：未找到创建者 sessionId={}", session.getId());
            return;
        }

        try {
            Room room = roomService.createPublicRoom(name, ownerId);

            // 添加参与者到房间
            if (participants != null) {
                for (String participantIdStr : participants) {
                    Long participantId = parseLongId(participantIdStr);
                    if (!participantId.equals(ownerId)) {
                        try {
                            roomService.joinRoom(room.getId(), participantId);
                            logger.info("用户 {} 被邀请加入房间 {}", participantId, room.getId());
                        } catch (RuntimeException e) {
                            logger.warn("邀请用户 {} 加入房间失败: {}", participantId, e.getMessage());
                        }
                    }
                }
            }

            // 通知创建者 - 将 ID 转为 String 避免前端精度丢失
            // 注意：前端 Room 接口使用 "id" 而不是 "roomId"
            Event createdEvent = new Event("room:created", Map.of(
                    "id", String.valueOf(room.getId()),
                    "name", room.getName(),
                    "type", room.getType(),
                    "ownerId", String.valueOf(room.getOwnerId()),
                    "createdAt", room.getCreatedAt()));
            sendToSession(session.getId(), createdEvent);

            // 通知所有被邀请的在线用户
            Map<String, Object> inviteData = new HashMap<>();
            inviteData.put("id", String.valueOf(room.getId()));
            inviteData.put("name", room.getName());
            inviteData.put("type", room.getType());
            inviteData.put("ownerId", String.valueOf(room.getOwnerId()));
            inviteData.put("createdAt", room.getCreatedAt());

            Event inviteEvent = new Event("room:invite", inviteData);

            if (participants != null) {
                for (String participantIdStr : participants) {
                    Long participantId = parseLongId(participantIdStr);
                    if (!participantId.equals(ownerId)) {
                        String participantSessionId = userSessionMap.get(participantId);
                        if (participantSessionId != null) {
                            sendToSession(participantSessionId, inviteEvent);
                            logger.info("通知用户 {} 被邀请加入房间 {}", participantId, room.getId());
                        }
                    }
                }
            }

        } catch (RuntimeException e) {
            logger.error("创建房间失败: {}", e.getMessage());
        }
    }

    private void handleRoomJoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long roomId = parseLongId(data.get("roomId"));
        Long userId = sessionUserMap.get(session.getId());

        if (userId == null) {
            logger.warn("加入房间失败：未找到用户 sessionId={}", session.getId());
            return;
        }

        try {
            roomService.joinRoom(roomId, userId);

            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // 通知用户加入成功 - 将 ID 转为 String 避免前端精度丢失
                Event joinedEvent = new Event("room:joined", Map.of(
                        "roomId", String.valueOf(roomId),
                        "userId", String.valueOf(userId)));
                sendToSession(session.getId(), joinedEvent);

                // 通知房间其他成员 - 将 ID 转为 String 避免前端精度丢失
                Event memberJoinedEvent = new Event("room:member:joined", Map.of(
                        "roomId", String.valueOf(roomId),
                        "user", Map.of("userId", String.valueOf(user.getId()), "username", user.getUsername())));
                broadcastToRoomMembers(roomId, memberJoinedEvent, session.getId());
            }
        } catch (RuntimeException e) {
            logger.error("加入房间失败: {}", e.getMessage());
        }
    }

    private void handleRoomLeave(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long roomId = parseLongId(data.get("roomId"));
        Long userId = sessionUserMap.get(session.getId());

        if (userId == null) {
            return;
        }

        try {
            roomService.leaveRoom(roomId, userId);

            // 通知房间其他成员 - 将 ID 转为 String 避免前端精度丢失
            Event memberLeftEvent = new Event("room:member:left", Map.of(
                    "roomId", String.valueOf(roomId),
                    "userId", String.valueOf(userId)));
            broadcastToRoomMembers(roomId, memberLeftEvent);
        } catch (RuntimeException e) {
            logger.error("离开房间失败: {}", e.getMessage());
        }
    }

    private void handleRoomList(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long userId = parseLongId(data.get("userId"));

        List<Room> rooms = roomService.getUserRooms(userId);
        List<Map<String, Object>> roomList = rooms.stream().map(room -> {
            Map<String, Object> roomMap = new HashMap<>();
            // 将 ID 转为 String 避免前端精度丢失
            roomMap.put("id", String.valueOf(room.getId()));
            roomMap.put("name", room.getName());
            roomMap.put("type", room.getType());
            roomMap.put("ownerId", String.valueOf(room.getOwnerId()));
            roomMap.put("createdAt", room.getCreatedAt());

            // 获取最后一条消息
            Optional<Message> lastMsg = messageService.getLatestMessage(room.getId());
            if (lastMsg.isPresent()) {
                Message msg = lastMsg.get();
                Map<String, Object> lastMessageMap = new HashMap<>();
                // 将 ID 转为 String 避免前端精度丢失
                lastMessageMap.put("id", String.valueOf(msg.getId()));
                lastMessageMap.put("content", msg.getContent());
                lastMessageMap.put("senderId", String.valueOf(msg.getSenderId()));
                lastMessageMap.put("senderName", messageService.getSenderName(userId, msg.getSenderId()));
                lastMessageMap.put("timestamp", msg.getTimestamp());
                lastMessageMap.put("type", msg.getType());
                roomMap.put("lastMessage", lastMessageMap);
            }

            // 私聊房间显示对方用户名
            if ("private".equals(room.getType())) {
                List<Long> memberIds = roomService.getRoomMemberIds(room.getId());
                Long partnerId = memberIds.stream()
                        .filter(id -> !id.equals(userId))
                        .findFirst()
                        .orElse(null);
                if (partnerId != null) {
                    roomMap.put("name", roomService.getPrivateRoomDisplayName(userId, room.getId(), room.getName()));
                }
            }

            return roomMap;
        }).collect(Collectors.toList());

        Event listEvent = new Event("room:list:response", Map.of("rooms", roomList));
        sendToSession(session.getId(), listEvent);
    }

    private void handlePrivateRoomStart(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long targetUserId = parseLongId(data.get("targetUserId"));
        Long userId = sessionUserMap.get(session.getId());

        if (userId == null) {
            logger.warn("发起私聊失败：未找到用户 sessionId={}", session.getId());
            return;
        }

        // 检查是否尝试与自己私聊
        if (userId.equals(targetUserId)) {
            logger.warn("发起私聊失败：不能与自己私聊 userId={}", userId);
            return;
        }

        try {
            Room room = roomService.getOrCreatePrivateRoom(userId, targetUserId);

            Map<String, Object> responseData = new HashMap<>();
            // 将 ID 转为 String 避免前端精度丢失
            // 注意：前端 Room 接口使用 "id" 而不是 "roomId"
            responseData.put("id", String.valueOf(room.getId()));
            // 使用 getPrivateRoomDisplayName 确保返回的是当前用户视角下对方的用户名/备注名
            responseData.put("name", roomService.getPrivateRoomDisplayName(userId, room.getId(), room.getName()));
            responseData.put("type", room.getType());
            responseData.put("createdAt", room.getCreatedAt());

            // 获取对方用户名
            Optional<User> targetUser = userService.findById(targetUserId);
            if (targetUser.isPresent()) {
                responseData.put("targetUsername", targetUser.get().getUsername());
            }

            Event responseEvent = new Event("room:private:created", responseData);
            sendToSession(session.getId(), responseEvent);

        } catch (RuntimeException e) {
            logger.error("发起私聊失败: {}", e.getMessage());
        }
    }

    private void handleRoomSync(WebSocketSession session, Map<String, Object> data) throws IOException {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rooms = (List<Map<String, Object>>) data.get("rooms");

        if (rooms == null) {
            return;
        }

        List<Map<String, Object>> syncMessages = new ArrayList<>();

        for (Map<String, Object> roomData : rooms) {
            Long roomId = parseLongId(roomData.get("roomId"));
            Long lastSeq = parseLongId(roomData.get("lastSeq"));

            List<Message> messages = messageService.getHistory(roomId, lastSeq);
            for (Message msg : messages) {
                Map<String, Object> msgMap = new HashMap<>();
                // 将 ID 转为 String 避免前端精度丢失
                msgMap.put("id", String.valueOf(msg.getId()));
                msgMap.put("roomId", String.valueOf(msg.getRoomId()));
                msgMap.put("senderId", String.valueOf(msg.getSenderId()));
                msgMap.put("senderName", messageService.getSenderName(sessionUserMap.get(session.getId()), msg.getSenderId()));
                msgMap.put("content", msg.getContent());
                msgMap.put("type", msg.getType());
                msgMap.put("seq", msg.getSeq());
                msgMap.put("timestamp", msg.getTimestamp());
                if ("file".equals(msg.getType())) {
                    msgMap.put("fileId", msg.getFileId());
                    msgMap.put("fileName", msg.getFileName());
                    msgMap.put("fileUrl", msg.getFileUrl());
                    msgMap.put("fileSize", msg.getFileSize());
                    msgMap.put("fileType", msg.getFileType());
                }
                syncMessages.add(msgMap);
            }
        }

        Event syncEvent = new Event("room:sync:response", Map.of("messages", syncMessages));
        sendToSession(session.getId(), syncEvent);
    }

    private void handleRoomInviteMember(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long roomId = parseLongId(data.get("roomId"));
        Long targetUserId = parseLongId(data.get("targetUserId"));
        Long inviterId = sessionUserMap.get(session.getId());

        if (inviterId == null) {
            logger.warn("邀请成员失败：未找到邀请者 sessionId={}", session.getId());
            sendToSession(session.getId(), new Event("room:invite:error", Map.of("message", "邀请失败，请重新进入群聊后再试")));
            return;
        }

        try {
            // 检查房间是否存在
            Room room = roomService.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("房间不存在"));

            if (!roomService.isRoomMember(roomId, inviterId)) {
                logger.warn("邀请成员失败：用户 {} 不是房间 {} 的成员", inviterId, roomId);
                sendToSession(session.getId(), new Event("room:invite:error", Map.of("message", "只有群成员才能邀请他人加入")));
                return;
            }

            // 检查目标用户是否已经是成员
            List<Long> memberIds = roomService.getRoomMemberIds(roomId);
            if (memberIds.contains(targetUserId)) {
                logger.warn("邀请成员失败：用户 {} 已经是房间 {} 的成员", targetUserId, roomId);
                sendToSession(session.getId(), new Event("room:invite:error", Map.of("message", "该用户已经在群聊中")));
                return;
            }

            // 添加成员到房间
            roomService.joinRoom(roomId, targetUserId);
            logger.info("用户 {} 被邀请加入房间 {}", targetUserId, roomId);

            // 通知被邀请的用户
            Map<String, Object> inviteData = new HashMap<>();
            inviteData.put("id", String.valueOf(room.getId()));
            inviteData.put("name", room.getName());
            inviteData.put("type", room.getType());
            inviteData.put("ownerId", String.valueOf(room.getOwnerId()));
            inviteData.put("createdAt", room.getCreatedAt());

            Event inviteEvent = new Event("room:invite", inviteData);
            String targetSessionId = userSessionMap.get(targetUserId);
            if (targetSessionId != null) {
                sendToSession(targetSessionId, inviteEvent);
                logger.info("通知用户 {} 被邀请加入房间 {}", targetUserId, roomId);
            }

            Optional<User> targetUser = userService.findById(targetUserId);
            if (targetUser.isPresent()) {
                Event memberJoinedEvent = new Event("room:member:joined", Map.of(
                        "roomId", String.valueOf(roomId),
                        "user", Map.of(
                                "userId", String.valueOf(targetUser.get().getId()),
                                "username", targetUser.get().getUsername())));
                broadcastToRoomMembers(roomId, memberJoinedEvent);
            }

            // 通知邀请者成功
            Event successEvent = new Event("room:invite:success", Map.of(
                    "roomId", String.valueOf(roomId),
                    "targetUserId", String.valueOf(targetUserId)));
            sendToSession(session.getId(), successEvent);

        } catch (RuntimeException e) {
            logger.error("邀请成员失败: {}", e.getMessage());
            sendToSession(session.getId(), new Event("room:invite:error", Map.of("message", e.getMessage())));
        }
    }

    private void handleMessageRead(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long roomId = parseLongId(data.get("roomId"));
        Long userId = sessionUserMap.get(session.getId());

        if (userId == null) {
            logger.warn("消息已读事件失败：未找到用户 sessionId={}", session.getId());
            return;
        }

        // 向房间内其他成员广播已读事件
        Event readEvent = new Event("message:read", Map.of(
                "roomId", String.valueOf(roomId),
                "userId", String.valueOf(userId)));
        broadcastToRoomMembers(roomId, readEvent, session.getId());
    }

    private void handleMessageHistory(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long roomId = parseLongId(data.get("roomId"));

        List<Message> messages = messageService.getAllMessages(roomId);
        List<Map<String, Object>> messageList = messages.stream().map(msg -> {
            Map<String, Object> map = new HashMap<>();
            // 将 ID 转为 String 避免前端精度丢失
            map.put("id", String.valueOf(msg.getId()));
            map.put("roomId", String.valueOf(msg.getRoomId()));
            map.put("senderId", String.valueOf(msg.getSenderId()));
            map.put("senderName", messageService.getSenderName(sessionUserMap.get(session.getId()), msg.getSenderId()));
            map.put("content", msg.getContent());
            map.put("type", msg.getType());
            map.put("seq", msg.getSeq());
            map.put("timestamp", msg.getTimestamp());
            if ("file".equals(msg.getType())) {
                map.put("fileId", msg.getFileId());
                map.put("fileName", msg.getFileName());
                map.put("fileUrl", msg.getFileUrl());
                map.put("fileSize", msg.getFileSize());
                map.put("fileType", msg.getFileType());
            }
            return map;
        }).collect(Collectors.toList());

        // 将 ID 转为 String 避免前端精度丢失
        Event historyEvent = new Event("message:history:response", Map.of(
                "roomId", String.valueOf(roomId),
                "messages", messageList));

        sendToSession(session.getId(), historyEvent);
    }

    private void handleUserList(WebSocketSession session) throws IOException {
        List<Map<String, Object>> onlineUserList = userSessionMap.keySet().stream()
                .map(this::buildOnlineUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Event userListEvent = new Event("user:list:response", Map.of(
                "users", onlineUserList));

        sendToSession(session.getId(), userListEvent);
    }

    private Map<String, Object> buildOnlineUser(Long userId) {
        return userService.findById(userId)
                .map(user -> buildOnlineUser(user.getId(), user.getUsername()))
                .orElse(null);
    }

    private Map<String, Object> buildOnlineUser(Long userId, String username) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", String.valueOf(userId));
        userMap.put("username", username);
        return userMap;
    }

    private void broadcastToAll(Event event) throws IOException {
        broadcastToAll(event, null);
    }

    private void broadcastToAll(Event event, String excludeSessionId) throws IOException {
        String message = objectMapper.writeValueAsString(event);
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (entry.getKey().equals(excludeSessionId)) {
                continue;
            }
            WebSocketSession session = entry.getValue();
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IllegalStateException | java.io.IOException e) {
                    logger.warn("发送消息失败，会话已关闭或异常: {}, 错误: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    private void broadcastToRoomMembers(Long roomId, Event event) throws IOException {
        broadcastToRoomMembers(roomId, event, null);
    }

    private void broadcastToRoomMembers(Long roomId, Event event, String excludeSessionId) throws IOException {
        List<Long> memberIds = roomService.getRoomMemberIds(roomId);
        String message = objectMapper.writeValueAsString(event);

        for (Long memberId : memberIds) {
            String sessionId = userSessionMap.get(memberId);
            if (sessionId == null || sessionId.equals(excludeSessionId)) {
                continue;
            }

            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (java.nio.channels.ClosedChannelException e) {
                    logger.warn("发送消息失败，通道已关闭: {}", sessionId);
                } catch (IOException e) {
                    logger.error("发送消息失败: {}", e.getMessage());
                }
            }
        }
    }

    private void sendToSession(String sessionId, Event event) {
        try {
            WebSocketSession session = sessions.get(sessionId);
            if (session != null && session.isOpen()) {
                String message = objectMapper.writeValueAsString(event);
                session.sendMessage(new TextMessage(message));
            }
        } catch (java.nio.channels.ClosedChannelException e) {
            logger.warn("发送消息失败，通道已关闭: {}", sessionId);
        } catch (IOException e) {
            logger.error("发送消息失败: {}", e.getMessage());
        }
    }

    // 获取在线用户列表（用于 AdminController）
    public List<Map<String, Object>> getOnlineUsers() {
        return userSessionMap.keySet().stream()
                .map(userId -> userService.findById(userId).orElse(null))
                .filter(Objects::nonNull)
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    // 将 userId 转为 String 避免前端精度丢失
                    userMap.put("userId", String.valueOf(user.getId()));
                    userMap.put("username", user.getUsername());
                    return userMap;
                })
                .collect(Collectors.toList());
    }

    public void notifyRoomMemberLeft(Long roomId, Long userId) {
        try {
            Event memberLeftEvent = new Event("room:member:left", Map.of(
                    "roomId", String.valueOf(roomId),
                    "userId", String.valueOf(userId)));
            broadcastToRoomMembers(roomId, memberLeftEvent);
        } catch (IOException e) {
            logger.error("广播房间成员离开事件失败: roomId={}, userId={}, error={}", roomId, userId, e.getMessage());
        }
    }

    public void banOnlineUser(Long userId, String reason) {
        String sessionId = userSessionMap.get(userId);
        if (sessionId == null) {
            return;
        }

        sendToSession(sessionId, new Event("user:banned", Map.of(
                "reason", reason == null ? "" : reason
        )));

        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                logger.error("关闭被封禁用户连接失败: {}", e.getMessage());
            }
        }
    }

    // 数据模型类
    static class Event {
        private String type;
        private Map<String, Object> data;

        public Event() {
        }

        public Event(String type, Map<String, Object> data) {
            this.type = type;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
}
