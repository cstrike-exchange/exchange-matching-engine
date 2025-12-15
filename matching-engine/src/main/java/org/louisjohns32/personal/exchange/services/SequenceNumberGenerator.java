package org.louisjohns32.personal.exchange.services;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SequenceNumberGenerator {

    ConcurrentHashMap<String, AtomicLong> sequenceMap = new ConcurrentHashMap<String, AtomicLong>();

    public Long getSequenceNumber(String symbol) {
        if  (sequenceMap.containsKey(symbol)) {
            return sequenceMap.get(symbol).getAndIncrement();
        }
        AtomicLong seq = new AtomicLong(1);
        sequenceMap.putIfAbsent(symbol, seq);
        return seq.getAndIncrement();
    }

    // TODO get most recent sequence number on start up (currently not recoverable from crash)

}
