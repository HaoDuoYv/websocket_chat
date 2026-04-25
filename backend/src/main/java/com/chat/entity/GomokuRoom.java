package com.chat.entity;

import java.util.ArrayList;
import java.util.List;

public class GomokuRoom {

    public static final int BOARD_SIZE = 15;

    private String roomId;
    private String roomName;
    private String password;
    private GomokuPlayer blackPlayer;
    private GomokuPlayer whitePlayer;
    private List<GomokuPlayer> spectators = new ArrayList<>();
    private int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
    private int currentTurn = 1; // 1=黑, 2=白
    private GameStatus status = GameStatus.WAITING;
    private int winner = 0; // 0=无/平局, 1=黑胜, 2=白胜
    private int[][] winLine;
    private List<int[]> moveHistory = new ArrayList<>();
    private long createdAt;
    private long lastActivityAt;
    private Integer disconnectedPlayer; // 1或2
    private Long disconnectedAt;

    public GomokuRoom() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.lastActivityAt = now;
    }

    public int getPlayerCount() {
        int count = 0;
        if (blackPlayer != null) count++;
        if (whitePlayer != null) count++;
        return count;
    }

    public boolean isFull() {
        return blackPlayer != null && whitePlayer != null;
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    public String getPlayerRole(String userId) {
        if (blackPlayer != null && blackPlayer.getUserId().equals(userId)) {
            return "black";
        }
        if (whitePlayer != null && whitePlayer.getUserId().equals(userId)) {
            return "white";
        }
        if (spectators.stream().anyMatch(s -> s.getUserId().equals(userId))) {
            return "spectator";
        }
        return null;
    }

    public void updateActivity() {
        this.lastActivityAt = System.currentTimeMillis();
    }

    // --- 内部类 GomokuPlayer ---

    public static class GomokuPlayer {
        private String userId;
        private String username;

        public GomokuPlayer() {
        }

        public GomokuPlayer(String userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    // --- 枚举 GameStatus ---

    public enum GameStatus {
        WAITING, PLAYING, FINISHED
    }

    // --- Getters and Setters ---

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public GomokuPlayer getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(GomokuPlayer blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public GomokuPlayer getWhitePlayer() {
        return whitePlayer;
    }

    public void setWhitePlayer(GomokuPlayer whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public List<GomokuPlayer> getSpectators() {
        return spectators;
    }

    public void setSpectators(List<GomokuPlayer> spectators) {
        this.spectators = spectators;
    }

    public int[][] getBoard() {
        return board;
    }

    public void setBoard(int[][] board) {
        this.board = board;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(int currentTurn) {
        this.currentTurn = currentTurn;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }

    public int[][] getWinLine() {
        return winLine;
    }

    public void setWinLine(int[][] winLine) {
        this.winLine = winLine;
    }

    public List<int[]> getMoveHistory() {
        return moveHistory;
    }

    public void setMoveHistory(List<int[]> moveHistory) {
        this.moveHistory = moveHistory;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(long lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Integer getDisconnectedPlayer() {
        return disconnectedPlayer;
    }

    public void setDisconnectedPlayer(Integer disconnectedPlayer) {
        this.disconnectedPlayer = disconnectedPlayer;
    }

    public Long getDisconnectedAt() {
        return disconnectedAt;
    }

    public void setDisconnectedAt(Long disconnectedAt) {
        this.disconnectedAt = disconnectedAt;
    }
}
