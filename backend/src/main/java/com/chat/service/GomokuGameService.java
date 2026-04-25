package com.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GomokuGameService {

    private static final Logger logger = LoggerFactory.getLogger(GomokuGameService.class);
    private static final int BOARD_SIZE = 15;
    private static final long INACTIVE_ROOM_TIMEOUT_MS = 5 * 60 * 1000; // 5分钟
    private static final long DISCONNECT_TIMEOUT_MS = 30 * 1000; // 30秒

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public GameRoom createRoom(String roomName, String password, Long userId, String username) {
        String roomId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        GameRoom room = new GameRoom(roomId, roomName, password, userId, username);
        rooms.put(roomId, room);
        logger.info("五子棋房间创建: roomId={}, roomName={}, creator={}", roomId, roomName, username);
        return room;
    }

    public GameRoom joinRoom(String roomId, Long userId, String username) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        if (room.isFull()) {
            throw new RuntimeException("房间已满");
        }
        if (room.getGameState() == GameState.PLAYING) {
            throw new RuntimeException("游戏进行中");
        }
        if (room.getGameState() == GameState.FINISHED) {
            throw new RuntimeException("游戏已结束，只能观战");
        }
        room.setPlayer2(userId, username);
        room.setGameState(GameState.PLAYING);
        room.setCurrentTurn(1); // 黑棋先手
        room.updateActivity();
        logger.info("玩家 {} 加入房间 {}", username, roomId);
        return room;
    }

    public GameRoom spectateRoom(String roomId, Long userId, String username) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        room.addSpectator(userId, username);
        room.updateActivity();
        logger.info("玩家 {} 观战房间 {}", username, roomId);
        return room;
    }

    public boolean checkPassword(String roomId, String password) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return false;
        }
        if (room.getPassword() == null || room.getPassword().isEmpty()) {
            return true;
        }
        return room.getPassword().equals(password);
    }

    public MoveResult makeMove(String roomId, Long userId, int row, int col) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return MoveResult.reject("房间不存在");
        }
        if (room.getGameState() != GameState.PLAYING) {
            return MoveResult.reject("游戏未在进行中");
        }

        int player = room.getPlayerNumber(userId);
        if (player == 0) {
            return MoveResult.reject("你不是游戏玩家");
        }
        if (room.getCurrentTurn() != player) {
            return MoveResult.reject("不是你的回合");
        }
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            return MoveResult.reject("落子位置超出棋盘范围");
        }
        if (room.getBoard()[row][col] != 0) {
            return MoveResult.reject("该位置已有棋子");
        }

        room.getBoard()[row][col] = player;
        room.addMove(row, col, player);
        room.updateActivity();

        List<int[]> winLine = checkWin(room.getBoard(), row, col, player);
        if (winLine != null) {
            room.setGameState(GameState.FINISHED);
            room.setWinner(player);
            room.setWinLine(winLine);
            logger.info("五子棋对局结束: roomId={}, winner={}", roomId, player);
            return MoveResult.win(player, winLine, room.getCurrentTurn());
        }

        if (isBoardFull(room.getBoard())) {
            room.setGameState(GameState.FINISHED);
            logger.info("五子棋平局: roomId={}", roomId);
            return MoveResult.draw();
        }

        room.setCurrentTurn(player == 1 ? 2 : 1);
        return MoveResult.ok(room.getCurrentTurn());
    }

    public GameRoom restartGame(String roomId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        room.resetBoard();
        room.setGameState(GameState.PLAYING);
        room.setCurrentTurn(1);
        room.setWinner(0);
        room.setWinLine(null);
        room.clearDisconnected();
        room.updateActivity();
        logger.info("五子棋重新开始: roomId={}", roomId);
        return room;
    }

    public GameRoom surrender(String roomId, Long userId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        if (room.getGameState() != GameState.PLAYING) {
            throw new RuntimeException("游戏未在进行中");
        }

        int player = room.getPlayerNumber(userId);
        if (player == 0) {
            throw new RuntimeException("你不是游戏玩家");
        }

        int winner = player == 1 ? 2 : 1;
        room.setGameState(GameState.FINISHED);
        room.setWinner(winner);
        room.updateActivity();
        logger.info("五子棋玩家 {} 认输，对方获胜: roomId={}, winner={}", userId, roomId, winner);
        return room;
    }

    public GameRoom spectatorToPlayer(String roomId, Long userId, String username) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }
        if (!room.isSpectator(userId)) {
            throw new RuntimeException("你不是观战者");
        }

        int playerNum = 0;
        if (room.getPlayer1Id() == null) {
            playerNum = 1;
            room.setPlayer1(userId, username);
        } else if (room.getPlayer2Id() == null) {
            playerNum = 2;
            room.setPlayer2(userId, username);
        } else {
            throw new RuntimeException("没有空位");
        }

        room.removeSpectator(userId);

        if ((room.getGameState() == GameState.WAITING || room.getGameState() == GameState.FINISHED)
                && room.getPlayer1Id() != null && room.getPlayer2Id() != null) {
            room.setGameState(GameState.PLAYING);
            room.setCurrentTurn(1);
            room.setWinner(0);
            room.setWinLine(null);
            room.resetBoard();
        }

        room.updateActivity();
        logger.info("观战者 {} 加入对局成为玩家{}: roomId={}", username, playerNum, roomId);
        return room;
    }

    public GameRoom leaveRoom(String roomId, Long userId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在");
        }

        int player = room.getPlayerNumber(userId);
        if (player == 1) {
            room.setPlayer1(null, null);
        } else if (player == 2) {
            room.setPlayer2(null, null);
        } else {
            room.removeSpectator(userId);
        }

        // 如果房间空了就移除
        if (room.getPlayer1Id() == null && room.getPlayer2Id() == null) {
            rooms.remove(roomId);
            logger.info("五子棋房间移除(无人): roomId={}", roomId);
            return room;
        }

        // 如果游戏进行中有人离开，游戏结束
        if (room.getGameState() == GameState.PLAYING && player > 0) {
            room.setGameState(GameState.FINISHED);
            int otherPlayer = player == 1 ? 2 : 1;
            room.setWinner(otherPlayer);
            logger.info("五子棋玩家离开，对方获胜: roomId={}, winner={}", roomId, otherPlayer);
        }

        room.updateActivity();
        return room;
    }

    /**
     * 标记断线玩家，而非直接移除。返回断线玩家所在的roomId，如果不在任何房间则返回null。
     */
    public String handleDisconnect(Long userId) {
        for (Map.Entry<String, GameRoom> entry : rooms.entrySet()) {
            GameRoom room = entry.getValue();
            int player = room.getPlayerNumber(userId);
            if (player > 0) {
                // 玩家断线，标记断线状态
                room.setDisconnectedPlayer(player);
                room.setDisconnectedAt(System.currentTimeMillis());
                room.updateActivity();
                logger.info("玩家 {} 断线，标记房间 {} 的玩家 {}", userId, entry.getKey(), player);
                return entry.getKey();
            } else if (room.isSpectator(userId)) {
                // 观战者断线直接移除
                room.removeSpectator(userId);
                room.updateActivity();
            }
        }
        return null;
    }

    /**
     * 断线重连。如果用户在30秒窗口内重连，恢复游戏状态。
     */
    public ReconnectResult reconnect(String roomId, Long userId) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return ReconnectResult.fail("房间不存在");
        }

        Integer disconnectedPlayer = room.getDisconnectedPlayer();
        if (disconnectedPlayer == null) {
            return ReconnectResult.fail("没有断线玩家");
        }

        int player = room.getPlayerNumber(userId);
        if (player != disconnectedPlayer) {
            return ReconnectResult.fail("你不是断线玩家");
        }

        Long disconnectedAt = room.getDisconnectedAt();
        if (disconnectedAt == null) {
            return ReconnectResult.fail("断线时间异常");
        }

        if (System.currentTimeMillis() - disconnectedAt > DISCONNECT_TIMEOUT_MS) {
            return ReconnectResult.fail("重连超时");
        }

        // 清除断线标记
        room.clearDisconnected();
        room.updateActivity();
        logger.info("玩家 {} 重连成功，房间 {}", userId, roomId);
        return ReconnectResult.ok(room);
    }

    public void clearDisconnectedForUser(Long userId) {
        for (GameRoom room : rooms.values()) {
            int player = room.getPlayerNumber(userId);
            if (player > 0 && room.getDisconnectedPlayer() != null && room.getDisconnectedPlayer() == player) {
                room.clearDisconnected();
                logger.info("清除玩家 {} 的断线标记，房间 {}", userId, room.getRoomId());
                return;
            }
        }
    }

    /**
     * 定时清理：1) 不活跃的空房间；2) 断线超时的玩家判定负。
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupInactiveRooms() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, GameRoom> entry : rooms.entrySet()) {
            GameRoom room = entry.getValue();

            // 检查断线超时
            if (room.getDisconnectedPlayer() != null && room.getDisconnectedAt() != null) {
                if (now - room.getDisconnectedAt() > DISCONNECT_TIMEOUT_MS) {
                    int disconnectedPlayer = room.getDisconnectedPlayer();
                    int otherPlayer = disconnectedPlayer == 1 ? 2 : 1;
                    room.setGameState(GameState.FINISHED);
                    room.setWinner(otherPlayer);
                    room.clearDisconnected();
                    logger.info("断线超时，判定断线方负: roomId={}, winner={}", entry.getKey(), otherPlayer);
                }
            }

            // 检查不活跃空房间：两个玩家都为null且超过5分钟无活动
            if (room.getPlayer1Id() == null && room.getPlayer2Id() == null
                    && now - room.getLastActivityAt() > INACTIVE_ROOM_TIMEOUT_MS) {
                toRemove.add(entry.getKey());
                logger.info("清理不活跃空房间: roomId={}", entry.getKey());
            }
        }

        for (String roomId : toRemove) {
            rooms.remove(roomId);
        }
    }

    public List<GameRoom> getRoomList() {
        return new ArrayList<>(rooms.values());
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public GameRoom findUserRoom(Long userId) {
        for (GameRoom room : rooms.values()) {
            if (userId.equals(room.getPlayer1Id()) || userId.equals(room.getPlayer2Id())) {
                return room;
            }
        }
        for (GameRoom room : rooms.values()) {
            if (room.isSpectator(userId)) {
                return room;
            }
        }
        return null;
    }

    private List<int[]> checkWin(int[][] board, int row, int col, int player) {
        int[][] directions = { { 0, 1 }, { 1, 0 }, { 1, 1 }, { 1, -1 } };
        for (int[] dir : directions) {
            List<int[]> line = new ArrayList<>();
            line.add(new int[] { row, col });
            // 正方向
            for (int i = 1; i < 5; i++) {
                int r = row + dir[0] * i;
                int c = col + dir[1] * i;
                if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE || board[r][c] != player) {
                    break;
                }
                line.add(new int[] { r, c });
            }
            // 反方向
            for (int i = 1; i < 5; i++) {
                int r = row - dir[0] * i;
                int c = col - dir[1] * i;
                if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE || board[r][c] != player) {
                    break;
                }
                line.add(new int[] { r, c });
            }
            if (line.size() >= 5) {
                return line;
            }
        }
        return null;
    }

    private boolean isBoardFull(int[][] board) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    // ========== 内部数据类 ==========

    public enum GameState {
        WAITING, PLAYING, FINISHED
    }

    public static class GameRoom {
        private final String roomId;
        private final String roomName;
        private final String password;
        private final long createdAt;
        private long lastActivityAt;
        private Long player1Id;
        private String player1Name;
        private Long player2Id;
        private String player2Name;
        private final Map<Long, String> spectators = new ConcurrentHashMap<>();
        private int[][] board;
        private final List<int[]> moveHistory = new ArrayList<>(); // [row, col, player]
        private GameState gameState;
        private int currentTurn;
        private int winner;
        private List<int[]> winLine;
        private Integer disconnectedPlayer; // 1 或 2
        private Long disconnectedAt; // 断线时间戳

        public GameRoom(String roomId, String roomName, String password, Long player1Id, String player1Name) {
            this.roomId = roomId;
            this.roomName = roomName;
            this.password = password;
            this.createdAt = System.currentTimeMillis();
            this.lastActivityAt = this.createdAt;
            this.player1Id = player1Id;
            this.player1Name = player1Name;
            this.gameState = GameState.WAITING;
            this.currentTurn = 1;
            this.resetBoard();
        }

        public void updateActivity() {
            this.lastActivityAt = System.currentTimeMillis();
        }

        public void resetBoard() {
            this.board = new int[BOARD_SIZE][BOARD_SIZE];
            this.moveHistory.clear();
        }

        public boolean isFull() {
            return player1Id != null && player2Id != null;
        }

        public int getPlayerNumber(Long userId) {
            if (userId == null)
                return 0;
            if (userId.equals(player1Id))
                return 1;
            if (userId.equals(player2Id))
                return 2;
            return 0;
        }

        public void setPlayer1(Long id, String name) {
            this.player1Id = id;
            this.player1Name = name;
        }

        public void setPlayer2(Long id, String name) {
            this.player2Id = id;
            this.player2Name = name;
        }

        public void addSpectator(Long userId, String username) {
            spectators.put(userId, username);
        }

        public void removeSpectator(Long userId) {
            spectators.remove(userId);
        }

        public boolean isSpectator(Long userId) {
            return spectators.containsKey(userId);
        }

        public void addMove(int row, int col, int player) {
            moveHistory.add(new int[] { row, col, player });
        }

        public int[] getLastMove() {
            return moveHistory.isEmpty() ? null : moveHistory.get(moveHistory.size() - 1);
        }

        public void removeLastMove() {
            if (!moveHistory.isEmpty()) {
                moveHistory.remove(moveHistory.size() - 1);
            }
        }

        public void setDisconnectedPlayer(int player) {
            this.disconnectedPlayer = player;
        }

        public void setDisconnectedAt(long timestamp) {
            this.disconnectedAt = timestamp;
        }

        public void clearDisconnected() {
            this.disconnectedPlayer = null;
            this.disconnectedAt = null;
        }

        // Getters
        public String getRoomId() {
            return roomId;
        }

        public String getRoomName() {
            return roomName;
        }

        public String getPassword() {
            return password;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public long getLastActivityAt() {
            return lastActivityAt;
        }

        public Long getPlayer1Id() {
            return player1Id;
        }

        public String getPlayer1Name() {
            return player1Name;
        }

        public Long getPlayer2Id() {
            return player2Id;
        }

        public String getPlayer2Name() {
            return player2Name;
        }

        public Map<Long, String> getSpectators() {
            return spectators;
        }

        public int[][] getBoard() {
            return board;
        }

        public List<int[]> getMoveHistory() {
            return moveHistory;
        }

        public GameState getGameState() {
            return gameState;
        }

        public void setGameState(GameState gameState) {
            this.gameState = gameState;
        }

        public int getCurrentTurn() {
            return currentTurn;
        }

        public void setCurrentTurn(int currentTurn) {
            this.currentTurn = currentTurn;
        }

        public int getWinner() {
            return winner;
        }

        public void setWinner(int winner) {
            this.winner = winner;
        }

        public List<int[]> getWinLine() {
            return winLine;
        }

        public void setWinLine(List<int[]> winLine) {
            this.winLine = winLine;
        }

        public Integer getDisconnectedPlayer() {
            return disconnectedPlayer;
        }

        public Long getDisconnectedAt() {
            return disconnectedAt;
        }
    }

    public static class MoveResult {
        private final boolean valid;
        private final String reason;
        private final boolean win;
        private final boolean draw;
        private final int winner;
        private final List<int[]> winLine;
        private final int currentTurn;

        private MoveResult(boolean valid, String reason, boolean win, boolean draw,
                int winner, List<int[]> winLine, int currentTurn) {
            this.valid = valid;
            this.reason = reason;
            this.win = win;
            this.draw = draw;
            this.winner = winner;
            this.winLine = winLine;
            this.currentTurn = currentTurn;
        }

        public static MoveResult ok(int currentTurn) {
            return new MoveResult(true, null, false, false, 0, null, currentTurn);
        }

        public static MoveResult reject(String reason) {
            return new MoveResult(false, reason, false, false, 0, null, 0);
        }

        public static MoveResult win(int winner, List<int[]> winLine, int currentTurn) {
            return new MoveResult(true, null, true, false, winner, winLine, currentTurn);
        }

        public static MoveResult draw() {
            return new MoveResult(true, null, false, true, 0, null, 0);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

        public boolean isWin() {
            return win;
        }

        public boolean isDraw() {
            return draw;
        }

        public int getWinner() {
            return winner;
        }

        public List<int[]> getWinLine() {
            return winLine;
        }

        public int getCurrentTurn() {
            return currentTurn;
        }
    }

    public static class ReconnectResult {
        private final boolean success;
        private final String reason;
        private final GameRoom room;

        private ReconnectResult(boolean success, String reason, GameRoom room) {
            this.success = success;
            this.reason = reason;
            this.room = room;
        }

        public static ReconnectResult ok(GameRoom room) {
            return new ReconnectResult(true, null, room);
        }

        public static ReconnectResult fail(String reason) {
            return new ReconnectResult(false, reason, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getReason() {
            return reason;
        }

        public GameRoom getRoom() {
            return room;
        }
    }
}
