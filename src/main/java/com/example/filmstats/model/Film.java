package com.example.filmstats.model;

import java.util.List;
import java.util.Objects;

/**
 * Main entity. A film has a title, a director (many-to-one), release year,
 * production country and a multi-valued list of genres.
 */
public final class Film {

    private final String title;
    private final Director director;
    private final Integer year;
    private final String country;
    private final List<String> genres;

    public Film(String title, Director director, Integer year, String country, List<String> genres) {
        this.title = Objects.requireNonNull(title, "title");
        this.director = director;
        this.year = year;
        this.country = country;
        this.genres = genres == null ? List.of() : List.copyOf(genres);
    }

    public String getTitle() {
        return title;
    }

    public Director getDirector() {
        return director;
    }

    public Integer getYear() {
        return year;
    }

    public String getCountry() {
        return country;
    }

    public List<String> getGenres() {
        return genres;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Film other)) return false;
        return title.equals(other.title)
                && Objects.equals(director, other.director)
                && Objects.equals(year, other.year)
                && Objects.equals(country, other.country)
                && genres.equals(other.genres);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, director, year, country, genres);
    }

    @Override
    public String toString() {
        return "Film{title='" + title + "', director=" + director + ", year=" + year
                + ", country='" + country + "', genres=" + genres + '}';
    }
}
