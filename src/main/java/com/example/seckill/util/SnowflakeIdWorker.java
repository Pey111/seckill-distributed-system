package com.example.seckill.util;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdWorker {

    private static final long START = 1704067200000L;
    private static final long MACHINE_BITS = 5L;
    private static final long DATACENTER_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_MACHINE = ~(-1L << MACHINE_BITS);
    private static final long MAX_DATACENTER = ~(-1L << DATACENTER_BITS);
    private static final long MACHINE_LEFT = SEQUENCE_BITS;
    private static final long DATACENTER_LEFT = SEQUENCE_BITS + MACHINE_BITS;
    private static final long TIMESTAMP_LEFT = SEQUENCE_BITS + MACHINE_BITS + DATACENTER_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long datacenterId = 1L;
    private final long machineId = 1L;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public synchronized long nextId() {
        long timestamp = currentTime();
        if (timestamp < lastTimestamp) {
            timestamp = lastTimestamp;
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0L) {
                timestamp = nextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - START) << TIMESTAMP_LEFT)
                | (datacenterId << DATACENTER_LEFT)
                | (machineId << MACHINE_LEFT)
                | sequence;
    }

    private long nextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTime();
        }
        return timestamp;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }
}
