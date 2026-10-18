package it.unicam.cs.mpgc.rpg130268.combat.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DamageResultTest
{
    @Test void resultKeepsItsValues()
    {
        DamageResult result = new DamageResult(12, true);

        assertEquals(12, result.amount());
        assertTrue(result.critical());
    }

    @Test void zeroDamageIsAllowed()
    {
        assertDoesNotThrow(() -> new DamageResult(0, false));
    }

    @Test void negativeDamageThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new DamageResult(-1, false));
    }
}
