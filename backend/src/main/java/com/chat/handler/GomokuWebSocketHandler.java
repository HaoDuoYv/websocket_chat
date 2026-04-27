package com.chat.handler;

import com.chat.entity.User;
import com.chat.service.GomokuGameService;
import com.chat.service.GomokuGameService.GameRoom;
import com.chat.service.GomokuGameService.GameState;
import com.chat.service.GomokuGameService.MoveResult;
import com.chat.service.GomokuGameService.ReconnectResult;
import com.chat.service.GomokuGameService.TimeoutEvent;
import com.chat.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
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
public class GomokuWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GomokuWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> lobbySessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private GomokuGameService gomokuGameService;

    @Autowired
    private UserService userService;

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

    /**
     * 替代 Map.of()，自动跳过 null 值，避免 NPE。
     */
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
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("五子棋 WebSocket 连接建立 - Session ID: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Event event = objectMapper.readValue(payload, Event.class);

        switch (event.getType()) {
            case "game:join":
                handleGameJoin(session, event.getData());
                break;
            case "game:room:create":
                handleRoomCreate(session, event.getData());
                break;
            case "game:room:join":
                handleRoomJoin(session, event.getData());
                break;
            case "game:room:spectate":
                handleRoomSpectate(session, event.getData());
                break;
            case "game:move":
                handleMove(session, event.getData());
                break;
            case "game:restart:request":
                handleRestartRequest(session, event.getData());
                break;
            case "game:restart:respond":
                handleRestartRespond(session, event.getData());
                break;
            case "game:chat:send":
                handleChatSend(session, event.getData());
                break;
            case "game:room:leave":
                handleRoomLeave(session, event.getData());
                break;
            case "game:reconnect":
                handleReconnect(session, event.getData());
                break;
            case "game:surrender":
                handleSurrender(session, event.getData());
                break;
            case "game:spectator:join:player":
                handleSpectatorJoinAsPlayer(session, event.getData());
                break;
            case "game:rejoin":
                handleRejoin(session, event.getData());
                break;
            default:
                logger.warn("未知事件类型: {}", event.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        logger.info("五子棋 WebSocket 连接关闭 - Session ID: {}, 状态: {}", sessionId, status);

        String userId = sessionUserMap.get(sessionId);

        sessions.remove(sessionId);
        lobbySessions.remove(sessionId);

        if (userId != null) {
            userSessionMap.remove(userId);
            sessionUserMap.remove(sessionId);

            Long uid = parseLongId(userId);

            for (Map.Entry<String, Set<String>> entry : roomSessions.entrySet()) {
                if (entry.getValue().remove(sessionId)) {
                    String roomId = entry.getKey();
                    if (entry.getValue().isEmpty()) {
                        roomSessions.remove(roomId);
                    }
                }
            }

            try {
                String roomId = gomokuGameService.handleDisconnect(uid);
                if (roomId != null) {
                    // 查找断线玩家的用户名
                    GameRoom disconnRoom = gomokuGameService.getRoom(roomId);
                    String disconnUsername = null;
                    if (disconnRoom != null) {
                        int playerNum = disconnRoom.getPlayerNumber(uid);
                        if (playerNum == 1) {
                            disconnUsername = disconnRoom.getPlayer1Name();
                        } else if (playerNum == 2) {
                            disconnUsername = disconnRoom.getPlayer2Name();
                        }
                    }

                    broadcastToRoom(roomId, new Event("game:player:disconnected", safeMap(
                            "roomId", roomId,
                            "userId", userId,
                            "username", disconnUsername)));
                    broadcastRoomListToLobby();
                }
            } catch (Exception e) {
                logger.error("断开连接处理失败: {}", e.getMessage());
            }
        }
    }

    // ========== 事件处理 ==========

    private void handleGameJoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long userId = parseLongId(data.get("userId"));
        String username = (String) data.get("username");

        logger.info("用户加入五子棋大厅 - 用户名: {}, userId: {}", username, userId);

        sessions.put(session.getId(), session);
        userSessionMap.put(String.valueOf(userId), session.getId());
        sessionUserMap.put(session.getId(), String.valueOf(userId));

        lobbySessions.put(session.getId(), session);

        List<Map<String, Object>> roomList = buildRoomList();
        sendToSession(session.getId(), new Event("game:room:list", safeMap("rooms", roomList)));

        broadcastToLobby(new Event("game:user:online", safeMap(
                "userId", String.valueOf(userId),
                "username", username)), session.getId());
    }

    private void handleRoomCreate(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomName = (String) data.get("roomName");
        String password = (String) data.get("password");
        Long userId = parseLongId(data.get("userId"));
        String username = (String) data.get("username");

        try {
            GameRoom room = gomokuGameService.createRoom(roomName, password, userId, username);
            String roomId = room.getRoomId();

            lobbySessions.remove(session.getId());
            roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());

            broadcastRoomListToLobby();

            sendToSession(session.getId(), new Event("game:room:created", buildRoomState(room)));

            // 广播玩家加入（创建者为黑方）
            broadcastToRoom(roomId, new Event("game:player:joined", safeMap(
                    "roomId", roomId,
                    "userId", String.valueOf(userId),
                    "username", username,
                    "playerNumber", 1,
                    "role", "black",
                    "player", safeMap("userId", String.valueOf(userId), "username", username))));
        } catch (RuntimeException e) {
            logger.error("创建五子棋房间失败: {}", e.getMessage());
            sendToSession(session.getId(), new Event("game:error", safeMap("message", e.getMessage())));
        }
    }

    private void handleRoomJoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomId = (String) data.get("roomId");
        Long userId = parseLongId(data.get("userId"));
        String username = (String) data.get("username");
        String password = (String) data.get("password");

        try {
            if (!gomokuGameService.checkPassword(roomId, password)) {
                sendToSession(session.getId(), new Event("game:room:join:failed", safeMap(
                        "reason", "密码错误")));
                return;
            }

            GameRoom room = gomokuGameService.joinRoom(roomId, userId, username);

            lobbySessions.remove(session.getId());
            roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());

            broadcastRoomListToLobby();

            broadcastToRoom(roomId, new Event("game:player:joined", safeMap(
                    "roomId", roomId,
                    "userId", String.valueOf(userId),
                    "username", username,
                    "playerNumber", 2,
                    "role", "white",
                    "player", safeMap("userId", String.valueOf(userId), "username", username))));

            sendToSession(session.getId(), new Event("game:room:joined", buildRoomState(room)));
        } catch (RuntimeException e) {
            logger.error("加入五子棋房间失败: {}", e.getMessage());

            // 如果游戏已结束或进行中，检查是否有空位可以补位
            if (e.getMessage() != null && (e.getMessage().contains("只能观战") || e.getMessage().contains("游戏进行中"))) {
                GameRoom roomForCheck = gomokuGameService.getRoom(roomId);
                if (roomForCheck != null && !roomForCheck.isFull()) {
                    try {
                        GameRoom updatedRoom = gomokuGameService.spectatorToPlayer(roomId, userId, username);
                        int playerNum = updatedRoom.getPlayerNumber(userId);

                        lobbySessions.remove(session.getId());
                        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());

                        broadcastToRoom(roomId, new Event("game:player:joined", safeMap(
                                "roomId", roomId,
                                "userId", String.valueOf(userId),
                                "username", username,
                                "playerNumber", playerNum,
                                "role", playerNum == 1 ? "black" : "white",
                                "player", safeMap("userId", String.valueOf(userId), "username", username))));

                        sendToSession(session.getId(), new Event("game:room:joined", buildRoomState(updatedRoom)));
                        broadcastRoomListToLobby();
                        return;
                    } catch (RuntimeException ex2) {
                        logger.error("补位加入失败，尝试观战: {}", ex2.getMessage());
                    }
                }

                // 补位失败则自动转为观战者
                try {
                    handleRoomSpectate(session, data);
                    return;
                } catch (Exception ex) {
                    sendToSession(session.getId(), new Event("game:room:join:failed",
                            safeMap("reason", "加入房间失败")));
                    return;
                }
            }

            // 检查是否已在房间中
            GameRoom existingRoom = gomokuGameService.findUserRoom(userId);
            if (existingRoom != null) {
                sendToSession(session.getId(), new Event("game:room:join:rejected", safeMap(
                        "reason", "你已在房间中", "roomId", existingRoom.getRoomId())));
                return;
            }
            sendToSession(session.getId(), new Event("game:room:join:failed", safeMap("reason", e.getMessage())));
        }
    }

    private void handleRoomSpectate(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomId = (String) data.get("roomId");
        Long userId = parseLongId(data.get("userId"));
        String username = (String) data.get("username");

        try {
            GameRoom room = gomokuGameService.spectateRoom(roomId, userId, username);

            roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());

            broadcastToRoom(roomId, new Event("game:spectator:joined", safeMap(
                    "roomId", roomId,
                    "userId", String.valueOf(userId),
                    "username", username,
                    "spectator", safeMap("userId", String.valueOf(userId), "username", username))));

            sendToSession(session.getId(), new Event("game:room:spectating", buildRoomState(room)));
        } catch (RuntimeException e) {
            logger.error("观战五子棋房间失败: {}", e.getMessage());
            sendToSession(session.getId(), new Event("game:error", safeMap("message", e.getMessage())));
        }
    }

    private void handleMove(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomId = (String) data.get("roomId");
        Long userId = parseLongId(data.get("userId"));
        int row = ((Number) data.get("row")).intValue();
        int col = ((Number) data.get("col")).intValue();

        MoveResult result = gomokuGameService.makeMove(roomId, userId, row, col);

        if (!result.isValid()) {
            sendToSession(session.getId(), new Event("game:move:rejected", safeMap(
                    "reason", result.getReason())));
            return;
        }

        GameRoom room = gomokuGameService.getRoom(roomId);
        int player = room.getPlayerNumber(userId);

        if (result.isWin()) {
            broadcastToRoom(roomId, new Event("game:move:made", safeMap(
                    "roomId", roomId,
                    "row", row,
                    "col", col,
                    "player", player,
                    "currentTurn", result.getCurrentTurn(),
                    "board", room.getBoard())));

            List<List<Integer>> winLineList = formatWinLine(result.getWinLine());
            broadcastToRoom(roomId, new Event("game:over", safeMap(
                    "roomId", roomId,
                    "winner", result.getWinner(),
                    "winLine", winLineList)));
        } else if (result.isDraw()) {
            broadcastToRoom(roomId, new Event("game:move:made", safeMap(
                    "roomId", roomId,
                    "row", row,
                    "col", col,
                    "player", player,
                    "currentTurn", result.getCurrentTurn(),
                    "board", room.getBoard())));

            broadcastToRoom(roomId, new Event("game:draw", safeMap("roomId", roomId)));
        } else {
            broadcastToRoom(roomId, new Event("game:move:made", safeMap(
                    "roomId", roomId,
                    "row", row,
                    "col", col,
                    "player", player,
                    "currentTurn", result.getCurrentTurn(),
                    "board", room.getBoard())));
        }
    }

    private void handleRestartRequest(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomId = (String) data.get("roomId");
        Long userId = parseLongId(data.get("userId"));

        GameRoom room = gomokuGameService.getRoom(roomId);
        if (room == null) {
            sendToSession(session.getId(), new Event("game:error", safeMap("message", "房间不存在")));
            return;
        }

        int player = room.getPlayerNumber(userId);
        int opponentPlayer = player == 1 ? 2 : 1;

        String fromUsername = player == 1 ? room.getPlayer1Name() : room.getPlayer2Name();

        Long opponentId = opponentPlayer == 1 ? room.getPlayer1Id() : room.getPlayer2Id();
        if (opponentId != null) {
            String opponentSessionId = userSessionMap.get(String.valueOf(opponentId));
            if (opponentSessionId != null) {
                sendToSession(opponentSessionId, new Event("game:restart:requested", safeMap(
                        "roomId", roomId,
                        "userId", String.valueOf(userId),
                        "fromUserId", String.valueOf(userId),
                        "fromUsername", fromUsername)));
            }
        }
    }

    private void handleRestartRespond(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomId = (String) data.get("roomId");
        Long userId = parseLongId(data.get("userId"));
        boolean accepted = (boolean) data.get("accepted");

        if (accepted) {
            try {
                GameRoom room = gomokuGameService.restartGame(roomId);
                broadcastToRoom(roomId, new Event("game:restarted", buildRoomState(room)));
            } catch (RuntimeException e) {
                sendToSession(session.getId(), new Event("game:error", safeMap("message", e.getMessage())));
            }
        } else {
            GameRoom room = gomokuGameService.getRoom(roomId);
            if (room != null) {
                int player = room.getPlayerNumber(userId);
                int opponentPlayer = player == 1 ? 2 : 1;
                Long opponentId = opponentPlayer == 1 ? room.getPlayer1Id() : room.getPlayer2Id();
                if (opponentId != null) {
                    String opponentSessionId = userSessionMap.get(String.valueOf(opponentId));
                    if (opponentSessionId != null) {
                        sendToSession(opponentSessionId, new Event("game:restart:rejected", safeMap(
                                "roomId", roomId)));
                    }
                }
            }
        }
    }

    private void handleChatSend(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomId = (String) data.get("roomId");
        Long userId = parseLongId(data.get("userId"));
        String content = (String) data.get("content");

        GameRoom room = gomokuGameService.getRoom(roomId);
        if (room == null) {
            return;
        }

        String role;
        int player = room.getPlayerNumber(userId);
        String senderName;

        if (player == 1) {
            role = "black";
            senderName = room.getPlayer1Name();
        } else if (player == 2) {
            role = "white";
            senderName = room.getPlayer2Name();
        } else {
            role = "spectator";
            senderName = room.getSpectators().get(userId);
        }

        if (senderName == null || senderName.isEmpty()) {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isPresent()) {
                senderName = userOpt.get().getUsername();
            } else {
                senderName = String.valueOf(userId);
            }
        }

        broadcastToRoom(roomId, new Event("game:chat:message", safeMap(
                "roomId", roomId,
                "senderId", String.valueOf(userId),
                "senderName", senderName,
                "role", role,
                "content", content,
                "timestamp", System.currentTimeMillis())));
    }

    private void handleRoomLeave(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomId = (String) data.get("roomId");
        Long userId = parseLongId(data.get("userId"));

        if (roomId == null)
            return;

        GameRoom room = gomokuGameService.getRoom(roomId);
        if (room == null)
            return;

        // 保存广播需要的信息（leaveRoom 可能移除房间）
        String username = null;
        int playerNumber = room.getPlayerNumber(userId);
        if (playerNumber == 1)
            username = room.getPlayer1Name();
        else if (playerNumber == 2)
            username = room.getPlayer2Name();

        // 从 roomSessions 移除
        Set<String> roomSessionSet = roomSessions.get(roomId);
        if (roomSessionSet != null) {
            roomSessionSet.remove(session.getId());
            if (roomSessionSet.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }

        // 调用 leaveRoom（可能移除房间）
        try {
            gomokuGameService.leaveRoom(roomId, userId);
        } catch (RuntimeException e) {
            logger.error("离开五子棋房间失败: {}", e.getMessage());
        }

        // 广播给房间内其他成员（如果房间还存在）
        room = gomokuGameService.getRoom(roomId);
        if (room != null) {
            broadcastToRoom(roomId, new Event("game:player:left", safeMap(
                    "userId", String.valueOf(userId),
                    "username", username)));

            // 如果因离开导致游戏结束，广播 game:over
            if (room.getGameState() == GameState.FINISHED && room.getWinner() > 0) {
                broadcastToRoom(roomId, new Event("game:over", safeMap(
                        "roomId", roomId,
                        "winner", room.getWinner())));
            }
        }

        // 广播房间列表更新给大厅
        broadcastRoomListToLobby();

        // 添加回 lobbySessions
        lobbySessions.put(session.getId(), session);
    }

    private void handleReconnect(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomId = (String) data.get("roomId");
        Long userId = parseLongId(data.get("userId"));

        ReconnectResult result = gomokuGameService.reconnect(roomId, userId);
        if (result.isSuccess()) {
            sessions.put(session.getId(), session);
            userSessionMap.put(String.valueOf(userId), session.getId());
            sessionUserMap.put(session.getId(), String.valueOf(userId));

            roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());

            sendToSession(session.getId(), new Event("game:room:joined", buildRoomState(result.getRoom())));

            broadcastToRoom(roomId, new Event("game:player:reconnected", safeMap(
                    "roomId", roomId,
                    "userId", String.valueOf(userId))));
        } else {
            sendToSession(session.getId(), new Event("game:reconnect:failed", safeMap(
                    "reason", result.getReason())));
        }
    }

    private void handleSurrender(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomId = (String) data.get("roomId");
        Long userId = parseLongId(data.get("userId"));

        try {
            GameRoom room = gomokuGameService.surrender(roomId, userId);

            broadcastToRoom(roomId, new Event("game:over", safeMap(
                    "roomId", roomId,
                    "winner", room.getWinner())));
        } catch (RuntimeException e) {
            logger.error("认输失败: {}", e.getMessage());
            sendToSession(session.getId(), new Event("game:error", safeMap("message", e.getMessage())));
        }
    }

    private void handleSpectatorJoinAsPlayer(WebSocketSession session, Map<String, Object> data) throws IOException {
        String roomId = (String) data.get("roomId");
        Long userId = parseLongId(data.get("userId"));
        String username = (String) data.get("username");

        try {
            GameRoom room = gomokuGameService.spectatorToPlayer(roomId, userId, username);
            int playerNum = room.getPlayerNumber(userId);

            broadcastToRoom(roomId, new Event("game:player:joined", safeMap(
                    "roomId", roomId,
                    "userId", String.valueOf(userId),
                    "username", username,
                    "playerNumber", playerNum,
                    "role", playerNum == 1 ? "black" : "white",
                    "player", safeMap("userId", String.valueOf(userId), "username", username))));

            sendToSession(session.getId(), new Event("game:room:joined", buildRoomState(room)));

            if (room.getPlayer1Id() != null && room.getPlayer2Id() != null) {
                broadcastToRoom(roomId, new Event("game:room:state:update", buildRoomState(room)), session.getId());
            }

            broadcastRoomListToLobby();
        } catch (RuntimeException e) {
            logger.error("观战者加入对局失败: {}", e.getMessage());
            sendToSession(session.getId(), new Event("game:error", safeMap("message", e.getMessage())));
        }
    }

    @EventListener
    public void handleTimeoutEvent(TimeoutEvent event) throws IOException {
        String roomId = event.getRoomId();
        GameRoom room = gomokuGameService.getRoom(roomId);
        if (room == null)
            return;

        Long timedOutUserId = event.getPlayer() == 1 ? room.getPlayer1Id() : room.getPlayer2Id();

        broadcastToRoom(roomId, new Event("game:timeout", safeMap(
                "roomId", roomId,
                "userId", timedOutUserId != null ? String.valueOf(timedOutUserId) : null,
                "message", "超时自动落子")));

        MoveResult result = event.getMoveResult();
        int player = event.getPlayer();
        int row = event.getRow();
        int col = event.getCol();

        if (result.isWin()) {
            broadcastToRoom(roomId, new Event("game:move:made", safeMap(
                    "roomId", roomId,
                    "row", row,
                    "col", col,
                    "player", player,
                    "currentTurn", result.getCurrentTurn(),
                    "board", room.getBoard())));

            List<List<Integer>> winLineList = formatWinLine(result.getWinLine());
            broadcastToRoom(roomId, new Event("game:over", safeMap(
                    "roomId", roomId,
                    "winner", result.getWinner(),
                    "winLine", winLineList)));
        } else if (result.isDraw()) {
            broadcastToRoom(roomId, new Event("game:move:made", safeMap(
                    "roomId", roomId,
                    "row", row,
                    "col", col,
                    "player", player,
                    "currentTurn", result.getCurrentTurn(),
                    "board", room.getBoard())));

            broadcastToRoom(roomId, new Event("game:draw", safeMap("roomId", roomId)));
        } else {
            broadcastToRoom(roomId, new Event("game:move:made", safeMap(
                    "roomId", roomId,
                    "row", row,
                    "col", col,
                    "player", player,
                    "currentTurn", result.getCurrentTurn(),
                    "board", room.getBoard())));
        }
    }

    private void handleRejoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long userId = parseLongId(data.get("userId"));
        String roomId = (String) data.get("roomId");

        // 先尝试查找用户所在房间
        GameRoom room = null;
        if (roomId != null) {
            room = gomokuGameService.getRoom(roomId);
        }
        if (room == null) {
            room = gomokuGameService.findUserRoom(userId);
        }

        if (room == null) {
            sendToSession(session.getId(), new Event("game:rejoin:failed",
                    safeMap("reason", "未找到房间")));
            return;
        }

        // 检查用户是否仍在房间中（作为玩家或观战者）
        int playerNumber = room.getPlayerNumber(userId);
        boolean isSpectator = room.isSpectator(userId);

        // 注册 session 映射
        sessionUserMap.put(session.getId(), String.valueOf(userId));
        userSessionMap.put(String.valueOf(userId), session.getId());

        // 从 lobbySessions 移除
        lobbySessions.remove(session.getId());

        // 添加到 roomSessions
        roomSessions.computeIfAbsent(room.getRoomId(), k -> ConcurrentHashMap.newKeySet()).add(session.getId());

        if (playerNumber > 0 || isSpectator) {
            sendToSession(session.getId(), new Event("game:room:joined", buildRoomState(room)));
            if (playerNumber > 0) {
                gomokuGameService.clearDisconnectedForUser(userId);
                broadcastToRoom(room.getRoomId(), new Event("game:player:reconnected", safeMap(
                        "roomId", room.getRoomId(),
                        "userId", String.valueOf(userId))));
            }
        } else {
            // 用户不在房间中，以观战者身份加入
            try {
                room = gomokuGameService.spectateRoom(room.getRoomId(), userId,
                        (String) data.get("username"));

                // 向重入者发送观战状态
                sendToSession(session.getId(), new Event("game:room:spectating", buildRoomState(room)));

                // 向房间内其他成员广播完整的 spectators 列表更新（避免 push 重复）
                List<Map<String, Object>> spectatorList = room.getSpectators().entrySet().stream()
                        .map(e -> safeMap("userId", String.valueOf(e.getKey()), "username", e.getValue()))
                        .collect(Collectors.toList());
                broadcastToRoom(room.getRoomId(), new Event("game:spectator:list:update", safeMap(
                        "roomId", room.getRoomId(),
                        "spectators", spectatorList)), session.getId());
            } catch (RuntimeException e) {
                sendToSession(session.getId(), new Event("game:rejoin:failed",
                        safeMap("reason", "加入房间失败: " + e.getMessage())));
            }
        }
    }

    // ========== 辅助方法 ==========

    private List<Map<String, Object>> buildRoomList() {
        return gomokuGameService.getRoomList().stream().map(room -> {
            Map<String, Object> map = new HashMap<>();
            map.put("roomId", room.getRoomId());
            map.put("roomName", room.getRoomName());
            map.put("hasPassword", room.getPassword() != null && !room.getPassword().isEmpty());
            map.put("player1Id", room.getPlayer1Id() != null ? String.valueOf(room.getPlayer1Id()) : null);
            map.put("player1Name", room.getPlayer1Name());
            map.put("player2Id", room.getPlayer2Id() != null ? String.valueOf(room.getPlayer2Id()) : null);
            map.put("player2Name", room.getPlayer2Name());
            map.put("spectatorCount", room.getSpectators().size());
            map.put("state", room.getGameState().name());
            map.put("createdAt", room.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> buildRoomState(GameRoom room) {
        Map<String, Object> state = new HashMap<>();
        state.put("roomId", room.getRoomId());
        state.put("roomName", room.getRoomName());
        state.put("player1Id", room.getPlayer1Id() != null ? String.valueOf(room.getPlayer1Id()) : null);
        state.put("player1Name", room.getPlayer1Name());
        state.put("player2Id", room.getPlayer2Id() != null ? String.valueOf(room.getPlayer2Id()) : null);
        state.put("player2Name", room.getPlayer2Name());
        state.put("spectators", room.getSpectators().entrySet().stream()
                .map(e -> safeMap("userId", String.valueOf(e.getKey()), "username", e.getValue()))
                .collect(Collectors.toList()));
        state.put("board", room.getBoard());
        state.put("state", room.getGameState().name());
        state.put("currentTurn", room.getCurrentTurn());
        state.put("winner", room.getWinner());
        state.put("moveHistory", room.getMoveHistory().stream()
                .map(m -> safeMap("row", m[0], "col", m[1], "player", m[2]))
                .collect(Collectors.toList()));
        return state;
    }

    private List<List<Integer>> formatWinLine(List<int[]> winLine) {
        if (winLine == null)
            return null;
        return winLine.stream()
                .map(p -> List.of(p[0], p[1]))
                .collect(Collectors.toList());
    }

    private void broadcastRoomListToLobby() throws IOException {
        List<Map<String, Object>> roomList = buildRoomList();
        broadcastToLobby(new Event("game:room:list", safeMap("rooms", roomList)));
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

    private void broadcastToRoom(String roomId, Event event) throws IOException {
        broadcastToRoom(roomId, event, null);
    }

    private void broadcastToRoom(String roomId, Event event, String excludeSessionId) throws IOException {
        Set<String> sessIds = roomSessions.get(roomId);
        if (sessIds == null || sessIds.isEmpty()) {
            return;
        }
        String message = objectMapper.writeValueAsString(event);
        for (String sessId : sessIds) {
            if (sessId.equals(excludeSessionId)) {
                continue;
            }
            WebSocketSession session = sessions.get(sessId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IllegalStateException | IOException e) {
                    logger.warn("发送消息失败，会话异常: {}, 错误: {}", sessId, e.getMessage());
                }
            }
        }
    }

    private void broadcastToLobby(Event event) throws IOException {
        broadcastToLobby(event, null);
    }

    private void broadcastToLobby(Event event, String excludeSessionId) throws IOException {
        String message = objectMapper.writeValueAsString(event);
        for (Map.Entry<String, WebSocketSession> entry : lobbySessions.entrySet()) {
            if (entry.getKey().equals(excludeSessionId)) {
                continue;
            }
            WebSocketSession session = entry.getValue();
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IllegalStateException | IOException e) {
                    logger.warn("发送消息失败，会话异常: {}, 错误: {}", entry.getKey(), e.getMessage());
                }
            }
        }
    }

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
