package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

import java.util.Objects;

// A second bar a fighter can have besides HP (and AP): Gunther's Ink, or whatever a future member gets.
// It is a picture of the moment, so whoever draws it needs to know nothing about the member that owns it
public record ResourceGauge(String label, int current, int max)
{
    public ResourceGauge
    {
        Objects.requireNonNull(label, "label must not be null");

        if (max < 0 || current < 0 || current > max)
            throw new IllegalArgumentException("current must be between 0 and max");
    }
}
