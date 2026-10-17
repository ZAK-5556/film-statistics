package com.example.filmstats.statistics;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Immutable result: entries sorted by count descending; ties are ordered by value ascending
 * so that the output is deterministic.
 */
public final class Statistics {

    private final Attribute attribute;
    private final List<StatisticsEntry> entries;

    private Statistics(Attribute attribute, List<StatisticsEntry> entries) {
        this.attribute = attribute;
        this.entries = List.copyOf(entries);
    }

    private static final Comparator<StatisticsEntry> BY_COUNT_DESC_THEN_VALUE =
            Comparator.comparingLong(StatisticsEntry::count).reversed()
                    .thenComparing(StatisticsEntry::value);

    public static Statistics of(Attribute attribute, Map<String, Long> counts) {
        List<StatisticsEntry> sorted = counts.entrySet().stream()
                .map(e -> new StatisticsEntry(e.getKey(), e.getValue()))
                .sorted(BY_COUNT_DESC_THEN_VALUE)
                .toList();
        return new Statistics(attribute, sorted);
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public List<StatisticsEntry> getEntries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
