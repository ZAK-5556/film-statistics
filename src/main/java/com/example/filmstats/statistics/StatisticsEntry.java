package com.example.filmstats.statistics;

/** One row of the statistics: an attribute value and how many times it occurred. */
public record StatisticsEntry(String value, long count) {
}
