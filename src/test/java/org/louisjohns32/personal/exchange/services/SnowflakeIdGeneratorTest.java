package org.louisjohns32.personal.exchange.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;


public class SnowflakeIdGeneratorTest {

    private static final long WORKER_ID = 10L;
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
            idGenerator.nextId();
        }

        when(mockTimeProvider.currentTimeMillis()).thenReturn(BASE_TIMESTAMP + 1);

        long id = idGenerator.nextId();
        assertEquals(0, extractSequence(id));
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

        long extractedWorkerId = (id >> 12) & 0x1F;

        assertEquals(WORKER_ID, extractedWorkerId);
    }

    private long extractSequence(long id) {
        return id & 0xFFF;
    }
}