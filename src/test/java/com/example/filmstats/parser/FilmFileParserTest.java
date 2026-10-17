package com.example.filmstats.parser;

import com.example.filmstats.model.Film;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmFileParserTest {

    private final FilmFileParser parser = new FilmFileParser();

    private static Path fileWith(String content) throws IOException {
        Path file = Files.createTempFile("films", ".json");
        file.toFile().deleteOnExit();
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void shouldParseFilmsFromFile() throws IOException {
        // given
        Path file = fileWith("""
                [
                  {"title": "Inception", "director": "Christopher Nolan", "year": 2010, "genres": "Sci-Fi, Thriller"},
                  {"title": "Parasite", "director": "Bong Joon-ho", "year": 2019, "country": "South Korea", "genres": "Drama"}
                ]
                """);
        List<Film> films = new ArrayList<>();

        // when
        long count = parser.parse(file, films::add);

        // then
        assertEquals(2, count);
        assertEquals("Inception", films.get(0).getTitle());
        assertEquals("Bong Joon-ho", films.get(1).getDirector().getName());
        assertEquals(List.of("Sci-Fi", "Thriller"), films.get(0).getGenres());
    }

    @Test
    void shouldParseEmptyArray() throws IOException {
        // given
        Path file = fileWith("[]");

        // when
        long count = parser.parse(file, film -> { });

        // then
        assertEquals(0, count);
    }

    @Test
    void shouldFailOnInvalidRecordWithFileNameInMessage() throws IOException {
        // given
        Path file = fileWith("[{\"director\": \"No title\"}]");

        // when
        IOException exception = assertThrows(IOException.class, () -> parser.parse(file, film -> { }));

        // then
        assertTrue(exception.getMessage().contains(file.getFileName().toString()));
    }

    @Test
    void shouldFailOnMalformedJson() throws IOException {
        // given
        Path unterminated = fileWith("[{\"title\": \"A\"");
        Path notArray = fileWith("{\"title\": \"A\"}");
        Path notObjects = fileWith("[1, 2, 3]");

        // when / then
        assertThrows(IOException.class, () -> parser.parse(unterminated, film -> { }));
        assertThrows(IOException.class, () -> parser.parse(notArray, film -> { }));
        assertThrows(IOException.class, () -> parser.parse(notObjects, film -> { }));
    }

    @Test
    void shouldFailOnMissingFile() {
        // given
        Path missing = Path.of("definitely-missing-file.json");

        // when / then
        assertThrows(IOException.class, () -> parser.parse(missing, film -> { }));
    }

    @Test
    void shouldStreamLargeFile() throws IOException {
        // given
        int records = 50_000;
        StringBuilder content = new StringBuilder("[");
        for (int i = 0; i < records; i++) {
            if (i > 0) {
                content.append(',');
            }
            content.append("{\"title\":\"F").append(i).append("\",\"genres\":\"A, B\"}");
        }
        content.append(']');
        Path file = fileWith(content.toString());
        long[] seen = {0};

        // when
        long count = parser.parse(file, film -> seen[0]++);

        // then
        assertEquals(records, count);
        assertEquals(records, seen[0]);
    }
}
