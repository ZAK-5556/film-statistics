package com.example.filmstats.service;

import com.example.filmstats.statistics.Attribute;
import com.example.filmstats.statistics.Statistics;
import com.example.filmstats.statistics.StatisticsEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatisticsServiceTest {

    private final StatisticsService service = new StatisticsService();

    private static Path directoryWithFilms() throws IOException {
        Path dir = Files.createTempDirectory("films");
        Files.writeString(dir.resolve("a.json"), """
                [{"title": "A", "director": "Nolan", "year": 2010, "country": "USA", "genres": "Sci-Fi, Drama"},
                 {"title": "B", "director": "Nolan", "year": 2014, "country": "USA", "genres": "Sci-Fi"}]
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("b.json"), """
                [{"title": "C", "director": "Bong", "year": 2019, "country": "South Korea", "genres": "Drama, Thriller"}]
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("notes.txt"), "[not json, must be ignored]", StandardCharsets.UTF_8);
        return dir;
    }

    @Test
    void shouldMergeStatisticsAcrossFiles() throws IOException {
        // given
        Path dir = directoryWithFilms();

        // when
        Statistics statistics = service.calculate(dir, Attribute.GENRES, 2);

        // then
        assertEquals(List.of(
                new StatisticsEntry("Drama", 2),
                new StatisticsEntry("Sci-Fi", 2),
                new StatisticsEntry("Thriller", 1)), statistics.getEntries());
    }

    @Test
    void shouldProduceSameResultForAnyThreadCount() throws IOException {
        // given
        Path dir = directoryWithFilms();

        // when
        List<StatisticsEntry> singleThread = service.calculate(dir, Attribute.DIRECTOR, 1).getEntries();
        List<StatisticsEntry> multiThread = service.calculate(dir, Attribute.DIRECTOR, 8).getEntries();

        // then
        assertEquals(singleThread, multiThread);
        assertEquals(new StatisticsEntry("Nolan", 2), singleThread.get(0));
    }

    @Test
    void shouldListOnlyJsonFiles() throws IOException {
        // given
        Path dir = directoryWithFilms();

        // when
        List<Path> files = StatisticsService.listJsonFiles(dir);

        // then
        assertEquals(List.of("a.json", "b.json"), files.stream().map(p -> p.getFileName().toString()).toList());
    }

    @Test
    void shouldFailOnDirectoryWithoutJsonFiles() throws IOException {
        // given
        Path empty = Files.createTempDirectory("empty");

        // when / then
        assertThrows(IOException.class, () -> service.calculate(empty, Attribute.YEAR, 1));
    }

    @Test
    void shouldFailOnMissingDirectory() {
        // given
        Path missing = Path.of("definitely-missing-directory");

        // when / then
        assertThrows(IOException.class, () -> service.calculate(missing, Attribute.YEAR, 1));
    }

    @Test
    void shouldRejectNonPositiveThreadCount() throws IOException {
        // given
        Path dir = directoryWithFilms();

        // when / then
        assertThrows(IllegalArgumentException.class, () -> service.calculate(dir, Attribute.YEAR, 0));
    }

    @Test
    void shouldPropagateParseErrorFromWorkerThread() throws IOException {
        // given
        Path dir = Files.createTempDirectory("bad");
        Files.writeString(dir.resolve("bad.json"), "[{\"title\": }]", StandardCharsets.UTF_8);

        // when / then
        assertThrows(IOException.class, () -> service.calculate(dir, Attribute.YEAR, 4));
    }
}
