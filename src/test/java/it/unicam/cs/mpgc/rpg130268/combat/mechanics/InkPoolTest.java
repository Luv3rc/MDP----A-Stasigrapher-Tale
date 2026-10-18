package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkPool;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InkPoolTest {
    private InkPool pool(int current) {
        return new InkPool(8, current, InkRules.defaults());
    }

    @Test
    void gainIsCappedAtMax() {
        InkPool p = pool(7);
        p.gainInk(5);
        assertEquals(8, p.getCurrentInk());
    }

    @Test
    void drainReturnsAmountAndEmptiesGauge() {
        InkPool p = pool(6);
        assertEquals(6, p.drainInk());
        assertEquals(0, p.getCurrentInk());
    }

    @Test
    void resetEmptiesGauge() {
        InkPool p = pool(5);
        p.resetInk();
        assertEquals(0, p.getCurrentInk());
    }

    @Test
    void overspendingThrows() {
        assertThrows(IllegalArgumentException.class, () -> pool(2).spendInk(3));
    }
}
