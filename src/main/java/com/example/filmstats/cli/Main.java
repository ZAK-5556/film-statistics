package com.example.filmstats.cli;

import com.example.filmstats.service.StatisticsService;
import com.example.filmstats.statistics.Statistics;
import com.example.filmstats.statistics.StatisticsEntry;
import com.example.filmstats.xml.XmlStatisticsWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Console entry point. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        CommandLineArgs options;
        try {
            options = CommandLineArgs.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(CommandLineArgs.USAGE);
            System.exit(2);
            return;
        }

        try {
            long start = System.nanoTime();
            Statistics statistics = new StatisticsService()
                    .calculate(options.directory(), options.attribute(), options.threads());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            Files.createDirectories(options.outputDirectory());
            Path output = new XmlStatisticsWriter().write(statistics, options.outputDirectory());

            System.out.printf("Statistics by '%s' (%d threads, %d ms):%n",
                    options.attribute().getJsonName(), options.threads(), elapsedMs);
            for (StatisticsEntry entry : statistics.getEntries()) {
                System.out.printf("  %-40s %d%n", entry.value(), entry.count());
            }
            System.out.println("Result written to " + output.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
