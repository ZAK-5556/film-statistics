package com.example.filmstats.statistics;

import com.example.filmstats.model.Film;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe accumulator of attribute occurrences. Several parser threads can call
 * {@link #add(Film)} concurrently; the result is read once all of them are done.
 */
public final class StatisticsCollector {

    private final Attribute attribute;
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();

    public StatisticsCollector(Attribute attribute) {
        this.attribute = attribute;
    }

    public void add(Film film) {
        for (String value : attribute.extract(film)) {
            counts.computeIfAbsent(value, k -> new LongAdder()).increment();
        }
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public Statistics toStatistics() {
        Map<String, Long> snapshot = new HashMap<>();
        counts.forEach((value, adder) -> snapshot.put(value, adder.sum()));
        return Statistics.of(attribute, snapshot);
    }
}
