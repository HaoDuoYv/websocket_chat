package com.chat.utils;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {
    // 自定义纪元：2024-01-01 00:00:00 UTC
    private static final long EPOCH = 1704067200000L;
    private static final long MACHINE_BITS = 10L;
    private static final long SEQ_BITS = 12L;
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_BITS);
    private static final long MAX_SEQ = ~(-1L << SEQ_BITS);

    private final long machineId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator() {
        this(0L);
    }

    public SnowflakeIdGenerator(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException(
                "machineId must be between 0 and " + MAX_MACHINE_ID);
        }
        this.machineId = machineId;
    }

    public synchronized long nextId() {
        long now = System.currentTimeMillis();
        if (now < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards");
        }
        if (now == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQ;
            if (sequence == 0) {
                now = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = now;
        return ((now - EPOCH) << (MACHINE_BITS + SEQ_BITS))
            | (machineId << SEQ_BITS)
            | sequence;
    }

    private long waitNextMillis(long last) {
        long now = System.currentTimeMillis();
        while (now <= last) {
            now = System.currentTimeMillis();
        }
        return now;
    }
}
