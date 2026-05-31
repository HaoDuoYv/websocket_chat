package com.chat.component;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupAssistantInvocationManager {

    public enum AcquireResult { OK, IN_FLIGHT, DEBOUNCED }

    public static final long DEBOUNCE_MS = 2000L;

    private final ConcurrentHashMap<Key, State> states = new ConcurrentHashMap<>();

    public AcquireResult tryAcquire(Long roomId, Long assistantId, long now) {
        Key key = new Key(roomId, assistantId);
        State state = states.computeIfAbsent(key, k -> new State());
        synchronized (state) {
            if (state.inFlight) {
                return AcquireResult.IN_FLIGHT;
            }
            if (state.lastInvokedAt > 0L && now - state.lastInvokedAt < DEBOUNCE_MS) {
                return AcquireResult.DEBOUNCED;
            }
            state.inFlight = true;
            state.lastInvokedAt = now;
            return AcquireResult.OK;
        }
    }

    public void release(Long roomId, Long assistantId) {
        Key key = new Key(roomId, assistantId);
        State state = states.get(key);
        if (state == null) return;
        synchronized (state) {
            state.inFlight = false;
        }
    }

    public void setInFlightMessageId(Long roomId, Long assistantId, String messageId) {
        State state = states.get(new Key(roomId, assistantId));
        if (state == null) return;
        synchronized (state) {
            state.inFlightMessageId = messageId;
        }
    }

    private record Key(Long roomId, Long assistantId) {
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(roomId, k.roomId) && Objects.equals(assistantId, k.assistantId);
        }
        @Override public int hashCode() { return Objects.hash(roomId, assistantId); }
    }

    private static class State {
        long lastInvokedAt = 0L;
        boolean inFlight = false;
        String inFlightMessageId;
    }
}
