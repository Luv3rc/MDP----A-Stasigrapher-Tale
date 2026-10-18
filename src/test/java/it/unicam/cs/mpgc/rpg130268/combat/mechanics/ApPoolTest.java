package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApPool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApPoolTest {
    @Test
    void gainIsCappedAtMax() {
        ApPool p = new ApPool(9, 8);
        p.gainAp(5);
        assertEquals(9, p.getCurrentAp());
    }

    @Test
    void spendReducesAp() {
        ApPool p = new ApPool(9, 5);
        p.spendAp(3);
        assertEquals(2, p.getCurrentAp());
    }

    @Test
    void overspendingThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ApPool(9, 2).spendAp(3));
    }

    @Test
    void fillApReachesMax() {
        ApPool p = new ApPool(9, 0);
        p.fillAp();
        assertEquals(9, p.getCurrentAp());
    }

    @Test
    void resetSetsValue() {
        ApPool p = new ApPool(9, 7);
        p.resetToStartingValue();
        assertEquals(7, p.getCurrentAp());
    }
}
