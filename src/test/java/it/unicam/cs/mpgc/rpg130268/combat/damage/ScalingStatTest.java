package it.unicam.cs.mpgc.rpg130268.combat.damage;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScalingStatTest
{
    // maxHealth 50, strength 8, defense 4, agility 6, luck 3
    private final Stats stats = new Stats(50, 8, 4, 6, 3);

    @Test void eachScalingStatReadsItsOwnValue()
    {
        assertEquals(50, ScalingStat.MAX_HEALTH.valueIn(stats));
        assertEquals(8, ScalingStat.STRENGTH.valueIn(stats));
        assertEquals(4, ScalingStat.DEFENSE.valueIn(stats));
        assertEquals(6, ScalingStat.AGILITY.valueIn(stats));
        assertEquals(3, ScalingStat.LUCK.valueIn(stats));
    }
}