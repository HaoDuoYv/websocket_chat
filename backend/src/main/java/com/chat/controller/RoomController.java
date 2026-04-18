package com.chat.controller;

import com.chat.entity.Message;
import com.chat.entity.Room;
import com.chat.entity.User;
import com.chat.service.MessageService;
import com.chat.service.RoomService;
import com.chat.service.UserService;
import com.chat.handler.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @GetMapping
    public ResponseEntity<?> getUserRooms(@RequestParam Long userId) {
        List<Room> rooms = roomService.getUserRooms(userId);
        List<Map<String, Object>> roomList = rooms.stream().map(room -> {
            Map<String, Object> map = new HashMap<>();
            // 将 Long 类型的 ID 转为 String，避免前端 JavaScript 精度丢失
            map.put("id", String.valueOf(room.getId()));
            map.put("name", room.getName());
            map.put("type", room.getType());
            map.put("ownerId", String.valueOf(room.getOwnerId()));
            map.put("createdAt", room.getCreatedAt());

            // 获取最后一条消息
            Optional<Message> lastMsg = messageService.getLatestMessage(room.getId());
            if (lastMsg.isPresent()) {
                Message msg = lastMsg.get();
                Map<String, Object> lastMessageMap = new HashMap<>();
                lastMessageMap.put("id", String.valueOf(msg.getId()));
                lastMessageMap.put("content", msg.getContent());
                lastMessageMap.put("senderId", String.valueOf(msg.getSenderId()));
                lastMessageMap.put("senderName", messageService.getSenderName(userId, msg.getSenderId()));
                lastMessageMap.put("timestamp", msg.getTimestamp());
                lastMessageMap.put("type", msg.getType());
                map.put("lastMessage", lastMessageMap);
            }

            // 私聊房间显示对方用户名
            if ("private".equals(room.getType())) {
                List<Long> memberIds = roomService.getRoomMemberIds(room.getId());
                Long partnerId = memberIds.stream()
                        .filter(id -> !id.equals(userId))
                        .findFirst()
                        .orElse(null);
                if (partnerId != null) {
                    map.put("name", roomService.getPrivateRoomDisplayName(userId, room.getId(), room.getName()));
                }
            }

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("rooms", roomList));
    }

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        // 支持 String 和 Number 类型的 ownerId
        Long ownerId = parseLongId(request.get("ownerId"));

        try {
            Room room = roomService.createPublicRoom(name, ownerId);
            Map<String, Object> response = new HashMap<>();
            // 将 Long 类型的 ID 转为 String，避免前端 JavaScript 精度丢失
            response.put("id", String.valueOf(room.getId()));
            response.put("name", room.getName());
            response.put("type", room.getType());
            response.put("ownerId", String.valueOf(room.getOwnerId()));
            response.put("createdAt", room.getCreatedAt());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable Long roomId, @RequestBody Map<String, Object> request) {
        // 支持 String 和 Number 类型的 userId
        Long userId = parseLongId(request.get("userId"));
        try {
            roomService.joinRoom(roomId, userId);
            return ResponseEntity.ok(Map.of("message", "加入房间成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable Long roomId, @RequestBody Map<String, Object> request) {
        // 支持 String 和 Number 类型的 userId
        Long userId = parseLongId(request.get("userId"));
        try {
            roomService.leaveRoom(roomId, userId);
            return ResponseEntity.ok(Map.of("message", "离开房间成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<?> getRoomMembers(@PathVariable Long roomId, @RequestParam Long userId) {
        try {
            List<Long> memberIds = roomService.getRoomMemberIds(roomId);
            List<Map<String, Object>> members = memberIds.stream()
                    .map(memberId -> userService.findById(memberId).orElse(null))
                    .filter(user -> user != null)
                    .map(user -> {
                        Map<String, Object> map = new HashMap<>();
                        // 将 Long 类型的 ID 转为 String，避免前端 JavaScript 精度丢失
                        map.put("userId", String.valueOf(user.getId()));
                        map.put("username", messageService.getSenderName(userId, user.getId()));
                        return map;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("members", members));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long roomId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long lastSeq) {
        try {
            List<Message> messages = messageService.getHistory(roomId, lastSeq);
            List<Map<String, Object>> messageList = messages.stream().map(msg -> {
                Map<String, Object> map = new HashMap<>();
                // 将 Long 类型的 ID 转为 String，避免前端 JavaScript 精度丢失
                map.put("id", String.valueOf(msg.getId()));
                map.put("roomId", String.valueOf(msg.getRoomId()));
                map.put("senderId", String.valueOf(msg.getSenderId()));
                map.put("senderName", messageService.getSenderName(userId, msg.getSenderId()));
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

            return ResponseEntity.ok(Map.of("messages", messageList));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/private")
    public ResponseEntity<?> getOrCreatePrivateRoom(@RequestBody Map<String, Object> request) {
        // 支持 String 和 Number 类型的 userId
        Long userId1 = parseLongId(request.get("userId1"));
        Long userId2 = parseLongId(request.get("userId2"));

        try {
            Room room = roomService.getOrCreatePrivateRoom(userId1, userId2);
            Map<String, Object> response = new HashMap<>();
            // 将 Long 类型的 ID 转为 String，避免前端 JavaScript 精度丢失
            response.put("id", String.valueOf(room.getId()));
            response.put("name", room.getName());
            response.put("type", room.getType());
            response.put("createdAt", room.getCreatedAt());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/kick")
    public ResponseEntity<?> kickMember(@PathVariable Long roomId, @RequestBody Map<String, Object> request) {
        // 支持 String 和 Number 类型的 ID
        Long ownerId = parseLongId(request.get("ownerId"));
        Long targetUserId = parseLongId(request.get("targetUserId"));

        if (ownerId == null || targetUserId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "参数错误"));
        }

        try {
            // 检查是否是群主
            Room room = roomService.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("房间不存在"));

            if (!room.getOwnerId().equals(ownerId)) {
                return ResponseEntity.status(403).body(Map.of("message", "只有群主可以踢出成员"));
            }

            // 不能踢出自己
            if (ownerId.equals(targetUserId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "不能踢出自己"));
            }

            // 踢出成员
            roomService.leaveRoom(roomId, targetUserId);
            chatWebSocketHandler.notifyRoomMemberLeft(roomId, targetUserId);

            return ResponseEntity.ok(Map.of("message", "踢出成员成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/dissolve")
    public ResponseEntity<?> dissolveRoom(@PathVariable Long roomId, @RequestBody Map<String, Object> request) {
        Long ownerId = parseLongId(request.get("ownerId"));

        if (ownerId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "参数错误"));
        }

        try {
            // 检查是否是群主
            Room room = roomService.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("房间不存在"));

            if (!room.getOwnerId().equals(ownerId)) {
                return ResponseEntity.
                        status(403).body(Map.of("message", "只有群主可以解散群聊"));
            }

            // 解散群聊（删除房间）
            roomService.deleteRoom(roomId);

            return ResponseEntity.ok(Map.of("message", "解散群聊成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

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
        throw new IllegalArgumentException("Invalid ID value: " + value + " (type: " + value.getClass().getName() + ")");
    }
}
