package com.example.filmstats.cli;

import com.example.filmstats.statistics.Attribute;

import java.nio.file.Path;

/**
 * Parsed command-line arguments:
 * <pre>
 *   &lt;directory&gt; &lt;attribute&gt; [--threads N] [--out DIR]
 * </pre>
 */
public record CommandLineArgs(Path directory, Attribute attribute, int threads, Path outputDirectory) {

    private static final String THREADS_OPTION = "--threads";
    private static final String OUT_OPTION = "--out";

    public static final String USAGE = """
            Usage: java -jar film-statistics.jar <directory> <attribute> [--threads N] [--out DIR]

              <directory>   folder with *.json files describing films
              <attribute>   attribute to build statistics for: %s
              --threads N   size of the parsing thread pool (default: number of CPU cores)
              --out DIR     where to put statistics_by_<attribute>.xml (default: <directory>)
            """.formatted(Attribute.supportedNames());

    public static CommandLineArgs parse(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Expected at least 2 arguments");
        }
        Path directory = Path.of(args[0]);
        Attribute attribute = Attribute.fromName(args[1]);
        int threads = Runtime.getRuntime().availableProcessors();
        Path out = directory;

        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case THREADS_OPTION -> threads = parseThreads(requireValue(args, ++i, THREADS_OPTION));
                case OUT_OPTION -> out = Path.of(requireValue(args, ++i, OUT_OPTION));
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }
        return new CommandLineArgs(directory, attribute, threads, out);
    }

    private static int parseThreads(String value) {
        int threads;
        try {
            threads = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(THREADS_OPTION + " must be a number, got '" + value + "'");
        }
        if (threads < 1) {
            throw new IllegalArgumentException(THREADS_OPTION + " must be >= 1");
        }
        return threads;
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + option);
        }
        return args[index];
    }
}
