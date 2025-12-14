package org.louisjohns32.personal.exchange.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;


public class SnowflakeIdGeneratorTest {

    private static final long WORKER_ID = 1000L;
    private static final long BASE_TIMESTAMP = 10000000L;

    private SnowflakeIdGenerator idGenerator;
    private TimeProvider mockTimeProvider;

    @BeforeEach
    public void setup() {
        mockTimeProvider = mock(TimeProvider.class);
        idGenerator = new SnowflakeIdGenerator(WORKER_ID, mockTimeProvider);
    }

    @Test
    public void sameMillisecond_incrementsSequence() {
        when(mockTimeProvider.currentTimeMillis()).thenReturn(BASE_TIMESTAMP);

        long id1 = idGenerator.nextId();
        long id2 = idGenerator.nextId();
        long id3 = idGenerator.nextId();

        assertNotEquals(id1, id2);
        assertNotEquals(id2, id3);
        assertTrue(id2 > id1);
        assertTrue(id3 > id2);

        assertEquals(0, extractSequence(id1));
        assertEquals(1, extractSequence(id2));
        assertEquals(2, extractSequence(id3));
    }

    @Test
    public void newTimestamp_resetsSequence() {
        when(mockTimeProvider.currentTimeMillis()).thenReturn(BASE_TIMESTAMP);
        idGenerator.nextId();
        idGenerator.nextId();
        long id1 = idGenerator.nextId();
        assertEquals(2, extractSequence(id1));

        when(mockTimeProvider.currentTimeMillis()).thenReturn(BASE_TIMESTAMP + 1);

        long id2 = idGenerator.nextId();

        assertEquals(0, extractSequence(id2));
        assertTrue(id2 > id1);
    }

    @Test
    public void laterTimestamp_resetsSequenceRegardlessOfPreviousValue() {
        when(mockTimeProvider.currentTimeMillis()).thenReturn(BASE_TIMESTAMP);
        for (int i = 0; i < 100; i++) {
            idGenerator.nextId();
        }
        long id1 = idGenerator.nextId();
        assertEquals(100, extractSequence(id1));

        when(mockTimeProvider.currentTimeMillis()).thenReturn(BASE_TIMESTAMP + 1000);

        long id2 = idGenerator.nextId();

        assertEquals(0, extractSequence(id2));
    }

    @Test
    public void sequenceOverflow_resetsOnNextTimestamp() {
        when(mockTimeProvider.currentTimeMillis()).thenReturn(BASE_TIMESTAMP);

        for (int i = 0; i < 4096; i++) {
            long id = idGenerator.nextId();
            assertEquals(i, extractSequence(id));
        }

        when(mockTimeProvider.currentTimeMillis()).thenReturn(BASE_TIMESTAMP, BASE_TIMESTAMP + 1);

        long id = idGenerator.nextId();
        assertEquals(0, extractSequence(id));
        assertEquals(BASE_TIMESTAMP + 1, extractTimestamp(id));
    }

    @Test
    public void multipleTimestamps_generatesUniqueIds() {
        Set<Long> ids = new HashSet<>();

        for (int ts = 0; ts < 10; ts++) {
            when(mockTimeProvider.currentTimeMillis()).thenReturn(BASE_TIMESTAMP + ts);
            for (int i = 0; i < 100; i++) {
                ids.add(idGenerator.nextId());
            }
        }

        assertEquals(1000, ids.size());
    }

    @Test
    public void generatedId_embedsWorkerId() {
        when(mockTimeProvider.currentTimeMillis()).thenReturn(BASE_TIMESTAMP);
        long id = idGenerator.nextId();

        long extractedWorkerId = (id >> 12) & 0x3FF;

        assertEquals(WORKER_ID, extractedWorkerId);
    }

    @Test
    void testWorkerIdValidation_tooLarge_throwsException() {
        long maxWorkerId = (1L << 10) - 1;  // 1023 for 10 bits

        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(maxWorkerId + 1, mockTimeProvider));
    }

    @Test
    void testWorkerIdValidation_negative_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(-1, mockTimeProvider));
    }

    @Test
    void testWorkerIdValidation_maxAllowed_succeeds() {
        long maxWorkerId = (1L << 10) - 1;  // 1023
        assertDoesNotThrow(() -> new SnowflakeIdGenerator(maxWorkerId, mockTimeProvider));
    }

    @Test
    void testClockRollback_throwsException() {
        when(mockTimeProvider.currentTimeMillis())
                .thenReturn(BASE_TIMESTAMP)
                .thenReturn(BASE_TIMESTAMP - 1);  // Clock moved backwards!

        idGenerator.nextId();  // First call succeeds

        assertThrows(IllegalStateException.class,
                () -> idGenerator.nextId(),
                "Clock moved backwards");
    }



    private long extractSequence(long id) {
        return id & 0xFFF;
    }
    private long extractTimestamp(long id) {
        return (id >> 22) & 0x1FFFFFFFFFFL;
    }
}