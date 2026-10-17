package com.example.filmstats.parser;

import com.example.filmstats.model.Director;
import com.example.filmstats.model.Film;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FilmMapper mapper = new FilmMapper();

    private static JsonNode json(String text) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(text);
    }

    @Test
    void shouldMapFullRecord() throws JsonProcessingException {
        // given
        JsonNode node = json("""
                {"title": " Inception ", "director": "Christopher Nolan", "year": 2010,
                 "country": "USA", "genres": "Sci-Fi, Thriller,Action, "}
                """);

        // when
        Film film = mapper.map(node);

        // then
        assertEquals("Inception", film.getTitle());
        assertEquals(new Director("Christopher Nolan"), film.getDirector());
        assertEquals(2010, film.getYear());
        assertEquals("USA", film.getCountry());
        assertEquals(List.of("Sci-Fi", "Thriller", "Action"), film.getGenres());
    }

    @Test
    void shouldSupportGenresAsArrayAndYearAsString() throws JsonProcessingException {
        // given
        JsonNode node = json("""
                {"title": "Dune", "year": "2021", "genres": ["Sci-Fi", " Adventure ", ""]}
                """);

        // when
        Film film = mapper.map(node);

        // then
        assertEquals(2021, film.getYear());
        assertEquals(List.of("Sci-Fi", "Adventure"), film.getGenres());
    }

    @Test
    void shouldSupportDirectorAsNestedObject() throws JsonProcessingException {
        // given
        JsonNode node = json("""
                {"title": "Dune", "director": {"name": "Denis Villeneuve"}}
                """);

        // when
        Film film = mapper.map(node);

        // then
        assertEquals("Denis Villeneuve", film.getDirector().getName());
    }

    @Test
    void shouldAllowMissingOptionalFields() throws JsonProcessingException {
        // given
        JsonNode node = json("""
                {"title": "Untitled", "year": null}
                """);

        // when
        Film film = mapper.map(node);

        // then
        assertNull(film.getDirector());
        assertNull(film.getYear());
        assertNull(film.getCountry());
        assertTrue(film.getGenres().isEmpty());
    }

    @Test
    void shouldRejectRecordWithoutTitle() throws JsonProcessingException {
        // given
        JsonNode noTitle = json("""
                {"director": "Someone"}
                """);
        JsonNode blankTitle = json("""
                {"title": "   "}
                """);

        // when / then
        assertThrows(IllegalArgumentException.class, () -> mapper.map(noTitle));
        assertThrows(IllegalArgumentException.class, () -> mapper.map(blankTitle));
    }

    @Test
    void shouldRejectInvalidYear() throws JsonProcessingException {
        // given
        JsonNode node = json("""
                {"title": "X", "year": "nineteen"}
                """);

        // when / then
        assertThrows(IllegalArgumentException.class, () -> mapper.map(node));
    }
}
