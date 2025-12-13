package org.louisjohns32.personal.exchange.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SnowflakeIdGenerator implements IdGenerator<Long> {

    private final long workerId;
    private final TimeProvider timeProvider;

    private static final long WORKER_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long MAX_WORKER_ID = (1L << WORKER_BITS) - 1;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    @Autowired
    public SnowflakeIdGenerator(@Value("${snowflake.worker-id}") long workerId) {
        this(workerId, TimeProvider.getSystemTimeProvider());
    }

    public SnowflakeIdGenerator(long workerId, TimeProvider timeProvider) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "Worker ID must be between 0 and " + MAX_WORKER_ID +
                            ", got: " + workerId
            );
        }
        this.workerId = workerId;
        this.timeProvider = timeProvider;
    }

    /**
     * Generates snowflake ID
     * First 41 bits are for timestamp millis, next 10 are for worker id and remaining are for sequence number.
     * @return snowflakeId
     */
    public synchronized Long nextId() {
        long timestamp = timeProvider.currentTimeMillis();

        if(timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards");
        }

        if(lastTimestamp == timestamp) {
            if(++sequence > MAX_SEQUENCE) {
                sequence = 0;
                timestamp = blockUntil(timestamp+1);
            }
        } else {
            sequence = 0L;
            lastTimestamp = timestamp;
        }

        return timestamp << (WORKER_BITS + SEQUENCE_BITS) | workerId <<  SEQUENCE_BITS | sequence;
    }

    private long blockUntil(long timestamp) {
        do  {
            lastTimestamp = timeProvider.currentTimeMillis();
        } while (lastTimestamp < timestamp);
        return lastTimestamp;
    }

}
