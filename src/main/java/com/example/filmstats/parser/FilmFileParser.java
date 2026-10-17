package com.example.filmstats.parser;

import com.example.filmstats.model.Film;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Streams films from a JSON file that contains a top-level array of film objects.
 * <p>
 * Uses the Jackson streaming API: the file is read token by token and only one film
 * object is materialised at a time, so memory usage does not depend on the file size.
 */
public final class FilmFileParser {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonFactory jsonFactory = objectMapper.getFactory();
    private final FilmMapper filmMapper = new FilmMapper();

    /**
     * Parses the file and passes every film to {@code consumer}.
     *
     * @return number of parsed films
     * @throws IOException if the file cannot be read, is not valid JSON or contains an invalid film
     */
    public long parse(Path file, Consumer<Film> consumer) throws IOException {
        try (JsonParser parser = jsonFactory.createParser(file.toFile())) {
            expectArrayStart(parser, file);
            long count = 0;
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                consumer.accept(readFilm(parser));
                count++;
            }
            expectArrayEnd(parser, file);
            return count;
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid film record in " + file + ": " + e.getMessage(), e);
        }
    }

    private Film readFilm(JsonParser parser) throws IOException {
        return filmMapper.map(objectMapper.readTree(parser));
    }

    private static void expectArrayStart(JsonParser parser, Path file) throws IOException {
        if (parser.nextToken() != JsonToken.START_ARRAY) {
            throw new IOException("Expected a JSON array at the top level of " + file);
        }
    }

    private static void expectArrayEnd(JsonParser parser, Path file) throws IOException {
        if (parser.currentToken() != JsonToken.END_ARRAY) {
            throw new IOException("Expected an array of JSON objects in " + file
                    + ", found " + parser.currentToken() + " at " + parser.currentLocation());
        }
    }
}
