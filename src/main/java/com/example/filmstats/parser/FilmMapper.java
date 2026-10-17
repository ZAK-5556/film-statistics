package com.example.filmstats.parser;

import com.example.filmstats.model.Director;
import com.example.filmstats.model.Film;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts one JSON object into a {@link Film}.
 */
public final class FilmMapper {

    public static final String TITLE = "title";
    public static final String DIRECTOR = "director";
    public static final String YEAR = "year";
    public static final String COUNTRY = "country";
    public static final String GENRES = "genres";

    private static final String GENRE_SEPARATOR = ",";

    public Film map(JsonNode json) {
        return new Film(
                mapTitle(json.get(TITLE)),
                mapDirector(json.get(DIRECTOR)),
                mapYear(json.get(YEAR)),
                mapText(json.get(COUNTRY)),
                mapGenres(json.get(GENRES)));
    }

    private static String mapTitle(JsonNode node) {
        String title = mapText(node);
        if (title == null) {
            throw new IllegalArgumentException("Film must have a non-empty string '" + TITLE + "'");
        }
        return title;
    }

    private static Director mapDirector(JsonNode node) {
        String name = node != null && node.isObject() ? mapText(node.get("name")) : mapText(node);
        return name == null ? null : new Director(name);
    }

    private static Integer mapYear(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        String text = mapText(node);
        if (text == null) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid '" + YEAR + "' value: " + text);
        }
    }

    /** Returns a trimmed text value, or {@code null} if the node is missing, not textual or blank. */
    private static String mapText(JsonNode node) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText().trim();
    }

    /**
     * Genres may be a comma-separated string ({@code "Drama, Thriller"}) or a JSON array of strings.
     */
    private static List<String> mapGenres(JsonNode node) {
        List<String> genres = new ArrayList<>();
        if (node == null) {
            return genres;
        }
        if (node.isTextual()) {
            for (String part : node.asText().split(GENRE_SEPARATOR)) {
                addIfNotBlank(genres, part);
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                addIfNotBlank(genres, item.asText());
            }
        }
        return genres;
    }

    private static void addIfNotBlank(List<String> target, String value) {
        if (!value.isBlank()) {
            target.add(value.trim());
        }
    }
}
