package it.unicam.cs.mpgc.rpg130268.combat.damage;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DamageSpecTest {

    @Test
    void propertiesAreSetCorrectly() {
        DamageSpec spec = new DamageSpec(StrikeWeight.HEAVY, 1.5);

        assertEquals(StrikeWeight.HEAVY, spec.weight());
        assertEquals(1.5, spec.bonusMultiplier());
    }

    @Test
    void standardConstructorSetsBonusToOne() {
        assertEquals(1.0, new DamageSpec(StrikeWeight.NORMAL).bonusMultiplier());
    }

    @Test
    void totalMultiplierIsWeightTimesBonus() {
        // HEAVY is 1.5x. With a 2.0 bonus, the total should be 3.0.
        assertEquals(3.0, new DamageSpec(StrikeWeight.HEAVY, 2.0).getTotalMultiplier());

        // LIGHT is 0.75x. With standard constructor (1.0 bonus), the total should be 0.75.
        assertEquals(0.75, new DamageSpec(StrikeWeight.LIGHT).getTotalMultiplier());
    }

    @Test
    void bonusCannotBeZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> new DamageSpec(StrikeWeight.NORMAL, 0));
        assertThrows(IllegalArgumentException.class, () -> new DamageSpec(StrikeWeight.NORMAL, -1.0));
    }

    @Test
    void strikeWeightCannotBeNull() {
        assertThrows(NullPointerException.class, () -> new DamageSpec(null, 1.0));
        assertThrows(NullPointerException.class, () -> new DamageSpec(null));
    }
}