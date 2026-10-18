package it.unicam.cs.mpgc.rpg130268.combat.damage;

import org.junit.jupiter.api.Test;

import static it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat.STRENGTH;
import static org.junit.jupiter.api.Assertions.*;

class DamageSpecTest
{
    @Test void specKeepsItsValues()
    {
        Scaling scaling = Scaling.of(STRENGTH);
        DamageSpec spec = new DamageSpec(scaling, 3, 1.5);

        assertEquals(scaling, spec.scaling());
        assertEquals(3, spec.hits());
        assertEquals(1.5, spec.multiplier());
    }

    @Test void atLeastOneHitIsRequired()
    {
        assertThrows(IllegalArgumentException.class, () -> new DamageSpec(Scaling.of(STRENGTH), 0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new DamageSpec(Scaling.of(STRENGTH), -1, 1.0));
    }

    @Test void multiplierMustBePositive()
    {
        assertThrows(IllegalArgumentException.class, () -> new DamageSpec(Scaling.of(STRENGTH), 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new DamageSpec(Scaling.of(STRENGTH), 1, -1.0));
    }

    @Test void missingScalingThrows()
    {
        assertThrows(NullPointerException.class, () -> new DamageSpec(null, 1, 1.0));
    }
}