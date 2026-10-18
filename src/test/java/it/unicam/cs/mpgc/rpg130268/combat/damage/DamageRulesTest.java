package it.unicam.cs.mpgc.rpg130268.combat.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DamageRulesTest
{
    // ---------- Creation ----------

    @Test void createsSuccessfullyWithValidParameters()
    {
        assertDoesNotThrow(() -> new DamageRules(0.01, 0.5, 1.5, 30, 1));
    }

    @Test void defaultsMatchTheDesign()
    {
        DamageRules rules = DamageRules.defaults();

        assertEquals(0.5 / DamageRules.STAT_CAP, rules.critChancePerLuck());
        assertEquals(0.5, rules.maxCritChance());
        assertEquals(2.0, rules.critMultiplier());
        assertEquals(30, rules.defenseDampening());
        assertEquals(1, rules.minDamage());
    }

    @Test void critChancePerLuckIsDerivedFromTheChanceAtTheStatCap()
    {
        assertEquals(0.6 / 60.0, DamageRules.critChancePerLuckToReach(0.6));
    }

    @Test void invalidChanceAtCapThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> DamageRules.critChancePerLuckToReach(0));
        assertThrows(IllegalArgumentException.class, () -> DamageRules.critChancePerLuckToReach(-0.5));
        assertThrows(IllegalArgumentException.class, () -> DamageRules.critChancePerLuckToReach(1.1));
    }

    @Test void invalidCritChancePerLuckThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new DamageRules(-0.2, 0.5, 2.0, 30, 1));
        assertThrows(IllegalArgumentException.class, () -> new DamageRules(-0.1, 0.5, 2.0, 30, 1));
    }

    @Test void invalidMaxCritChanceThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new DamageRules(0.01, -0.1, 2.0, 30, 1));
        assertThrows(IllegalArgumentException.class, () -> new DamageRules(0.01, 1.1, 2.0, 30, 1));
    }

    @Test void critMultiplierMustIncreaseDamage()
    {
        assertThrows(IllegalArgumentException.class, () -> new DamageRules(0.01, 0.5, 1.0, 30, 1));
        assertThrows(IllegalArgumentException.class, () -> new DamageRules(0.01, 0.5, 0.5, 30, 1));
    }

    @Test void invalidDefenseDampeningThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new DamageRules(0.01, 0.5, 2.0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new DamageRules(0.01, 0.5, 2.0, -10, 1));
    }

    @Test void invalidMinDamageThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new DamageRules(0.01, 0.5, 2.0, 30, 0));
        assertThrows(IllegalArgumentException.class, () -> new DamageRules(0.01, 0.5, 2.0, 30, -5));
    }

    // ---------- Critical chance ----------

    @Test void critChanceGrowsWithLuck()
    {
        DamageRules rules = new DamageRules(0.01, 0.5, 2.0, 30, 1);

        assertEquals(0.0, rules.critChance(0));
        assertEquals(0.1, rules.critChance(10), 1e-9);
        assertEquals(0.3, rules.critChance(30), 1e-9);
    }

    @Test void critChanceIsCappedAtTheMaximum()
    {
        DamageRules rules = new DamageRules(0.01, 0.5, 2.0, 30, 1);

        assertEquals(0.5, rules.critChance(50));
        assertEquals(0.5, rules.critChance(500));
    }

    // ---------- Defense ----------

    @Test void noDefenseLetsEverythingThrough()
    {
        assertEquals(1.0, DamageRules.defaults().damageTakenFactor(0));
    }

    @Test void defenseEqualToTheDampeningHalvesTheDamage()
    {
        assertEquals(0.5, DamageRules.defaults().damageTakenFactor(30));
    }

    @Test void defenseHasDiminishingReturnsAndNeverReachesZero()
    {
        DamageRules rules = DamageRules.defaults();

        double at30 = rules.damageTakenFactor(30);
        double at60 = rules.damageTakenFactor(60);
        double at600 = rules.damageTakenFactor(600);

        assertTrue(at60 < at30);
        assertTrue(at600 < at60);
        assertTrue(at600 > 0);
        // the first 30 points of defense are worth more than the next 30
        assertTrue((1.0 - at30) > (at30 - at60));
    }
}