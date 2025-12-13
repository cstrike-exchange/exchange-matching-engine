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

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    @Autowired
    public SnowflakeIdGenerator(@Value("${snowflake.worker-id}") long workerId) {
        this.workerId = workerId;
        this.timeProvider = TimeProvider.getSystemTimeProvider();
    }

    public SnowflakeIdGenerator(long workerId, TimeProvider timeProvider) {
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

        if(lastTimestamp == timestamp) {
            sequence++;
        } else {
            sequence = 0L;
            lastTimestamp = timestamp;
        }

        return timestamp << 64-(WORKER_BITS + SEQUENCE_BITS) | workerId <<  SEQUENCE_BITS | sequence;
    }

}
