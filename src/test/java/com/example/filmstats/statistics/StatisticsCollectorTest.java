package com.example.filmstats.statistics;

import com.example.filmstats.model.Director;
import com.example.filmstats.model.Film;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsCollectorTest {

    private static Film film(String director, Integer year, String country, String... genres) {
        return new Film("title", director == null ? null : new Director(director), year, country, List.of(genres));
    }

    @Test
    void shouldCountSingleValuedAttribute() {
        // given
        StatisticsCollector collector = new StatisticsCollector(Attribute.DIRECTOR);
        collector.add(film("Nolan", 2010, "USA", "Sci-Fi"));
        collector.add(film("Nolan", 2014, "USA", "Drama"));
        collector.add(film("Bong", 2019, "KR", "Drama"));

        // when
        List<StatisticsEntry> entries = collector.toStatistics().getEntries();

        // then
        assertEquals(List.of(new StatisticsEntry("Nolan", 2), new StatisticsEntry("Bong", 1)), entries);
    }

    @Test
    void shouldCountEveryValueOfMultiValuedAttribute() {
        // given
        StatisticsCollector collector = new StatisticsCollector(Attribute.GENRES);
        collector.add(film("A", 2000, "X", "Drama", "Thriller"));
        collector.add(film("B", 2001, "X", "Drama"));
        collector.add(film("C", 2002, "X", "Comedy", "Drama"));

        // when
        List<StatisticsEntry> entries = collector.toStatistics().getEntries();

        // then
        assertEquals(List.of(
                new StatisticsEntry("Drama", 3),
                new StatisticsEntry("Comedy", 1),
                new StatisticsEntry("Thriller", 1)), entries);
    }

    @Test
    void shouldSortByCountDescendingThenByValue() {
        // given
        StatisticsCollector collector = new StatisticsCollector(Attribute.YEAR);
        collector.add(film("A", 2020, "X"));
        collector.add(film("A", 1999, "X"));
        collector.add(film("A", 1999, "X"));
        collector.add(film("A", 2005, "X"));

        // when
        List<String> values = collector.toStatistics().getEntries().stream().map(StatisticsEntry::value).toList();

        // then
        assertEquals(List.of("1999", "2005", "2020"), values);
    }

    @Test
    void shouldIgnoreFilmsWithMissingAttribute() {
        // given
        StatisticsCollector collector = new StatisticsCollector(Attribute.COUNTRY);
        collector.add(film(null, null, null));
        collector.add(film("A", 2000, "USA"));

        // when
        List<StatisticsEntry> entries = collector.toStatistics().getEntries();

        // then
        assertEquals(List.of(new StatisticsEntry("USA", 1)), entries);
    }

    @Test
    void shouldReturnEmptyStatisticsWhenNothingAdded() {
        // given
        StatisticsCollector collector = new StatisticsCollector(Attribute.GENRES);

        // when
        Statistics statistics = collector.toStatistics();

        // then
        assertTrue(statistics.isEmpty());
        assertEquals(Attribute.GENRES, statistics.getAttribute());
    }

    @Test
    void shouldBeThreadSafe() throws InterruptedException {
        // given
        StatisticsCollector collector = new StatisticsCollector(Attribute.GENRES);
        int threads = 8;
        int filmsPerThread = 10_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        // when
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> addFilms(collector, filmsPerThread));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        // then
        List<StatisticsEntry> entries = collector.toStatistics().getEntries();
        long expected = (long) threads * filmsPerThread;
        assertEquals(List.of(new StatisticsEntry("Drama", expected), new StatisticsEntry("Thriller", expected)), entries);
    }

    private static void addFilms(StatisticsCollector collector, int count) {
        for (int i = 0; i < count; i++) {
            collector.add(film("D", 2000, "X", "Drama", "Thriller"));
        }
    }

    @Test
    void shouldResolveAttributeByNameIgnoringCase() {
        // when / then
        assertEquals(Attribute.GENRES, Attribute.fromName("Genres"));
        assertEquals(Attribute.YEAR, Attribute.fromName("year"));
    }

    @Test
    void shouldRejectUnknownAttribute() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Attribute.fromName("rating"));
    }
}
