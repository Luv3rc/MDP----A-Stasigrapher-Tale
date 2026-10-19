package it.unicam.cs.mpgc.rpg130268.combat.reaction;

import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HitTest
{
    @Test void hitKeepsItsValues()
    {
        Hit hit = new Hit(2000, ReactionOptions.PARRY_ONLY, StrikeWeight.HEAVY);

        assertEquals(2000, hit.windUpMs());
        assertEquals(ReactionOptions.PARRY_ONLY, hit.options());
        assertEquals(StrikeWeight.HEAVY, hit.strikeWeight());
    }

    @Test void immediateHitIsAllowed()
    {
        assertDoesNotThrow(() -> new Hit(0, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL));
    }

    @Test void negativeWindUpThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new Hit(-1, ReactionOptions.PARRY_ONLY, StrikeWeight.NORMAL));
    }

    @Test void missingOptionsThrows()
    {
        assertThrows(NullPointerException.class, () -> new Hit(500, null, StrikeWeight.NORMAL));
    }

    @Test void missingStrikeWeightThrows()
    {
        assertThrows(NullPointerException.class, () -> new Hit(500, ReactionOptions.PARRY_ONLY, null));
    }

    @Test void exampleComboBuilds()
    {
        List<Hit> combo = List.of(
                new Hit(2000, ReactionOptions.PARRY_ONLY, StrikeWeight.NORMAL),
                new Hit(400,  ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                new Hit(3000, ReactionOptions.PARRY_ONLY, StrikeWeight.EXTREME));

        assertEquals(3, combo.size());
    }
}