package com.example.filmstats.service;

import com.example.filmstats.parser.FilmFileParser;
import com.example.filmstats.statistics.Attribute;
import com.example.filmstats.statistics.Statistics;
import com.example.filmstats.statistics.StatisticsCollector;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * Orchestrates the whole calculation: finds JSON files in a directory, parses each
 * file in its own task on a fixed thread pool and merges results into one {@link Statistics}.
 */
public final class StatisticsService {

    private final FilmFileParser parser = new FilmFileParser();

    public Statistics calculate(Path directory, Attribute attribute, int threads) throws IOException {
        List<Path> files = listJsonFiles(directory);
        if (files.isEmpty()) {
            throw new IOException("No *.json files found in " + directory);
        }
        return calculate(files, attribute, threads);
    }

    public Statistics calculate(List<Path> files, Attribute attribute, int threads) throws IOException {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be >= 1");
        }
        StatisticsCollector collector = new StatisticsCollector(attribute);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (Path file : files) {
                Callable<Long> task = () -> parser.parse(file, collector::add);
                futures.add(executor.submit(task));
            }
            for (Future<Long> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while parsing files", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            throw new IOException("Failed to parse files: " + cause.getMessage(), cause);
        } finally {
            executor.shutdownNow();
        }
        return collector.toStatistics();
    }

    public static List<Path> listJsonFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Not a directory: " + directory);
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
