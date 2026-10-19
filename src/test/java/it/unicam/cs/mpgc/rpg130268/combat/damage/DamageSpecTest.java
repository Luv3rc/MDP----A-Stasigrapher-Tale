package it.unicam.cs.mpgc.rpg130268.combat.damage;

import org.junit.jupiter.api.Test;

import static it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat.STRENGTH;
import static org.junit.jupiter.api.Assertions.*;

class DamageSpecTest
{
    @Test void specKeepsItsValues()
    {
        Scaling scaling = Scaling.of(STRENGTH);
        DamageSpec spec = new DamageSpec(scaling, StrikeWeight.HEAVY, 1.5);

        assertEquals(scaling, spec.scaling());
        assertEquals(StrikeWeight.HEAVY, spec.weight());
        assertEquals(1.5, spec.bonusMultiplier());
    }

    @Test void withoutABonusTheBonusIsOne()
    {
        assertEquals(1.0, new DamageSpec(Scaling.of(STRENGTH), StrikeWeight.NORMAL).bonusMultiplier());
    }

    @Test void totalMultiplierIsTheWeightTimesTheBonus()
    {
        // HEAVY is 1.5, so 1.5 * 2.0
        assertEquals(3.0, new DamageSpec(Scaling.of(STRENGTH), StrikeWeight.HEAVY, 2.0).getTotalMultiplier());
        assertEquals(0.75, new DamageSpec(Scaling.of(STRENGTH), StrikeWeight.LIGHT).getTotalMultiplier());
    }

    @Test void bonusMustBePositive()
    {
        assertThrows(IllegalArgumentException.class, () -> new DamageSpec(Scaling.of(STRENGTH), StrikeWeight.NORMAL, 0));
        assertThrows(IllegalArgumentException.class, () -> new DamageSpec(Scaling.of(STRENGTH), StrikeWeight.NORMAL, -1.0));
    }

    @Test void missingScalingOrWeightThrows()
    {
        assertThrows(NullPointerException.class, () -> new DamageSpec(null, StrikeWeight.NORMAL, 1.0));
        assertThrows(NullPointerException.class, () -> new DamageSpec(Scaling.of(STRENGTH), null, 1.0));
    }
}