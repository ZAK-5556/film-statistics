package com.example.filmstats.benchmark;

import com.example.filmstats.service.StatisticsService;
import com.example.filmstats.statistics.Attribute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Measures parsing time for 1, 2, 4 and 8 threads.
 * <pre>
 *   java -cp target/classes com.example.filmstats.benchmark.BenchmarkRunner [files] [filmsPerFile] [runs]
 * </pre>
 */
public final class BenchmarkRunner {

    private static final int[] THREAD_COUNTS = {1, 2, 4, 8};

    private BenchmarkRunner() {
    }

    public static void main(String[] args) throws IOException {
        int files = args.length > 0 ? Integer.parseInt(args[0]) : 16;
        int filmsPerFile = args.length > 1 ? Integer.parseInt(args[1]) : 200_000;
        int runs = args.length > 2 ? Integer.parseInt(args[2]) : 3;

        Path dir = Files.createTempDirectory("film-benchmark");
        try {
            System.out.printf("Generating %d files x %d films...%n", files, filmsPerFile);
            new TestDataGenerator(42).generate(dir, files, filmsPerFile);
            long totalBytes;
            try (Stream<Path> s = Files.list(dir)) {
                totalBytes = s.mapToLong(p -> p.toFile().length()).sum();
            }
            System.out.printf("Total size: %.1f MB, CPU cores: %d%n%n",
                    totalBytes / 1_048_576.0, Runtime.getRuntime().availableProcessors());

            StatisticsService service = new StatisticsService();
            // warm-up so JIT compilation does not distort the first measurement
            service.calculate(dir, Attribute.GENRES, 4);

            System.out.println("| Threads | Best, ms | Avg, ms | Speedup vs 1 thread |");
            System.out.println("|---------|----------|---------|---------------------|");
            double baseline = -1;
            for (int threads : THREAD_COUNTS) {
                long best = Long.MAX_VALUE;
                long sum = 0;
                for (int r = 0; r < runs; r++) {
                    long start = System.nanoTime();
                    service.calculate(dir, Attribute.GENRES, threads);
                    long elapsed = (System.nanoTime() - start) / 1_000_000;
                    best = Math.min(best, elapsed);
                    sum += elapsed;
                }
                double avg = (double) sum / runs;
                if (baseline < 0) {
                    baseline = avg;
                }
                System.out.printf("| %7d | %8d | %7.0f | %18.2fx |%n", threads, best, avg, baseline / avg);
            }
        } finally {
            try (Stream<Path> s = Files.list(dir)) {
                s.forEach(p -> p.toFile().delete());
            }
            Files.deleteIfExists(dir);
        }
    }
}
