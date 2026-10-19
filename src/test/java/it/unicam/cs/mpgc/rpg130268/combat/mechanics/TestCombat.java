package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatsTest {
    @Test void validStatsAreCreated()
    {
        Stats s = new Stats(50, 8, 4, 6, 3);
        assertEquals(50, s.maxHealth());
    }
    @Test void zeroMaxHealthThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Stats(0, 1, 1, 1, 1));
    }
    @Test void negativeStatThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Stats(10, -1, 1, 1, 1));
    }
}


