package com.example.filmstats.statistics;

import com.example.filmstats.model.Film;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Film attributes that statistics can be calculated for.
 * Each attribute knows how to extract its value(s) from a {@link Film}.
 */
public enum Attribute {

    DIRECTOR("director", film -> film.getDirector() == null ? List.of() : List.of(film.getDirector().getName())),
    YEAR("year", film -> film.getYear() == null ? List.of() : List.of(String.valueOf(film.getYear()))),
    COUNTRY("country", film -> film.getCountry() == null ? List.of() : List.of(film.getCountry())),
    /** Multi-valued attribute: one film contributes to every genre it belongs to. */
    GENRES("genres", Film::getGenres);

    private final String jsonName;
    private final Function<Film, List<String>> extractor;

    Attribute(String jsonName, Function<Film, List<String>> extractor) {
        this.jsonName = jsonName;
        this.extractor = extractor;
    }

    public String getJsonName() {
        return jsonName;
    }

    public List<String> extract(Film film) {
        return extractor.apply(film);
    }

    public static Attribute fromName(String name) {
        for (Attribute attribute : values()) {
            if (attribute.jsonName.equalsIgnoreCase(name)) {
                return attribute;
            }
        }
        throw new IllegalArgumentException("Unsupported attribute '" + name + "'. Supported: " + supportedNames());
    }

    public static String supportedNames() {
        return Arrays.stream(values()).map(Attribute::getJsonName).collect(Collectors.joining(", "));
    }
}
