package com.example.filmstats.model;

import java.util.Objects;

/**
 * Secondary entity. Many films relate to one director (many-to-one).
 */
public final class Director {

    private final String name;

    public Director(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Director other && name.equals(other.name));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
