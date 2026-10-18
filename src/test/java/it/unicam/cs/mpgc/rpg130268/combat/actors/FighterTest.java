package it.unicam.cs.mpgc.rpg130268.combat.actors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FighterTest {
    private Fighter fighter() {
        return new Enemy("Goblin", new Stats(20, 5, 2, 4, 1));
    }

    @Test
    void startsAtFullHp() {
        assertEquals(20, fighter().getHp());
    }

    @Test
    void hpNeverGoesBelowZero() {
        Fighter f = fighter();
        f.takeDamage(999);
        assertEquals(0, f.getHp());
        assertFalse(f.isAlive());
    }

    @Test
    void healNeverExceedsMax() {
        Fighter f = fighter();
        f.takeDamage(5);
        f.heal(999);
        assertEquals(20, f.getHp());
    }

    @Test
    void negativeDamageThrows() {
        assertThrows(IllegalArgumentException.class, () -> fighter().takeDamage(-1));
    }
}
