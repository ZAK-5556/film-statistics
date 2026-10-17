package com.example.filmstats.benchmark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Generates large synthetic JSON files with films for benchmarking. */
public final class TestDataGenerator {

    private static final String[] DIRECTORS = {
            "Christopher Nolan", "Denis Villeneuve", "Greta Gerwig", "Bong Joon-ho", "Hayao Miyazaki",
            "Quentin Tarantino", "Sofia Coppola", "Wes Anderson", "Ridley Scott", "Agnieszka Holland",
            "Alfonso Cuaron", "Jane Campion", "Park Chan-wook", "Pedro Almodovar", "Lynne Ramsay"
    };
    private static final String[] COUNTRIES = {
            "USA", "UK", "France", "South Korea", "Japan", "Canada", "Germany", "Spain", "Poland", "Italy"
    };
    private static final String[] GENRES = {
            "Drama", "Thriller", "Sci-Fi", "Comedy", "Romance", "Horror", "Action",
            "Animation", "Documentary", "Crime", "Fantasy", "Mystery"
    };

    private final Random random;

    public TestDataGenerator(long seed) {
        this.random = new Random(seed);
    }

    /** Writes {@code fileCount} files with {@code filmsPerFile} records each into {@code directory}. */
    public void generate(Path directory, int fileCount, int filmsPerFile) throws IOException {
        Files.createDirectories(directory);
        for (int f = 0; f < fileCount; f++) {
            Path file = directory.resolve(String.format("films_%03d.json", f));
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write("[\n");
                for (int i = 0; i < filmsPerFile; i++) {
                    if (i > 0) {
                        writer.write(",\n");
                    }
                    writer.write(randomFilm(f, i));
                }
                writer.write("\n]\n");
            }
        }
    }

    private String randomFilm(int fileIndex, int index) {
        int genreCount = 1 + random.nextInt(3);
        StringBuilder genres = new StringBuilder();
        for (int g = 0; g < genreCount; g++) {
            if (g > 0) {
                genres.append(", ");
            }
            genres.append(GENRES[random.nextInt(GENRES.length)]);
        }
        return "  {\"title\": \"Film " + fileIndex + "-" + index + "\", "
                + "\"director\": \"" + DIRECTORS[random.nextInt(DIRECTORS.length)] + "\", "
                + "\"year\": " + (1950 + random.nextInt(76)) + ", "
                + "\"country\": \"" + COUNTRIES[random.nextInt(COUNTRIES.length)] + "\", "
                + "\"genres\": \"" + genres + "\"}";
    }
}
