package it.unicam.cs.mpgc.rpg130268.combat.reaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HitTest
{
    @Test void hitKeepsItsValues()
    {
        Hit hit = new Hit(2000, ReactionOptions.PARRY_ONLY, 1.5);

        assertEquals(2000, hit.windUpMs());
        assertEquals(ReactionOptions.PARRY_ONLY, hit.options());
        assertEquals(1.5, hit.powerMultiplier());
    }

    @Test void powerDefaultsToOne()
    {
        assertEquals(1.0, new Hit(500, ReactionOptions.DODGE_ONLY).powerMultiplier());
    }

    @Test void immediateHitIsAllowed()
    {
        assertDoesNotThrow(() -> new Hit(0, ReactionOptions.PARRY_OR_DODGE));
    }

    @Test void negativeWindUpThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new Hit(-1, ReactionOptions.PARRY_ONLY));
    }

    @Test void zeroOrNegativePowerThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new Hit(500, ReactionOptions.PARRY_ONLY, 0));
        assertThrows(IllegalArgumentException.class, () -> new Hit(500, ReactionOptions.PARRY_ONLY, -1));
    }

    @Test void missingOptionsThrows()
    {
        assertThrows(NullPointerException.class, () -> new Hit(500, null));
    }

    @Test
    void exampleComboBuilds()
    {
        List<Hit> combo = List.of(
                new Hit(2000, ReactionOptions.PARRY_ONLY),
                new Hit(400,  ReactionOptions.PARRY_OR_DODGE),
                new Hit(3000, ReactionOptions.PARRY_ONLY, 1.5));

        assertEquals(3, combo.size());
    }
}