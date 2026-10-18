package it.unicam.cs.mpgc.rpg130268.combat.damage;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat.*;
import static org.junit.jupiter.api.Assertions.*;

class ScalingTest
{
    // maxHealth 50, strength 8, defense 4, agility 6, luck 3
    private final Stats stats = new Stats(50, 8, 4, 6, 3);

    @Test void singleStatScalingUsesTheWholeStat()
    {
        assertEquals(8.0, Scaling.of(STRENGTH).valueFor(stats));
        assertEquals(6.0, Scaling.of(AGILITY).valueFor(stats));
    }

    @Test void weightScalesTheStat()
    {
        assertEquals(4.0, Scaling.of(STRENGTH, 0.5).valueFor(stats));
        assertEquals(12.0, Scaling.of(STRENGTH, 1.5).valueFor(stats));   // weights may exceed 1
    }

    @Test void severalStatsAreSummedWithTheirWeights()
    {
        Scaling scaling = Scaling.of(STRENGTH, 0.6).and(AGILITY, 0.4);

        assertEquals(8 * 0.6 + 6 * 0.4, scaling.valueFor(stats), 1e-9);
    }

    @Test void weightsDoNotHaveToAddUpToOne()
    {
        Scaling scaling = Scaling.of(STRENGTH).and(AGILITY, 1.0);

        assertEquals(14.0, scaling.valueFor(stats));
    }

    @Test void andReturnsANewScalingAndLeavesTheOriginalUntouched()
    {
        Scaling original = Scaling.of(STRENGTH);
        Scaling extended = original.and(LUCK, 2.0);

        assertEquals(8.0, original.valueFor(stats));
        assertEquals(8.0 + 6.0, extended.valueFor(stats));
    }

    @Test void addingTheSameStatTwiceThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> Scaling.of(STRENGTH).and(STRENGTH, 0.5));
    }

    @Test void emptyScalingThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new Scaling(Map.of()));
    }

    @Test void zeroOrNegativeWeightThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> Scaling.of(STRENGTH, 0));
        assertThrows(IllegalArgumentException.class, () -> Scaling.of(STRENGTH, -1));
        assertThrows(IllegalArgumentException.class, () -> Scaling.of(STRENGTH).and(AGILITY, -0.5));
    }

    @Test void missingPartsThrow()
    {
        assertThrows(NullPointerException.class, () -> Scaling.of(null));
        assertThrows(NullPointerException.class, () -> new Scaling(null));
        assertThrows(NullPointerException.class, () -> Scaling.of(STRENGTH).valueFor(null));
    }

    @Test void changingTheOriginalMapDoesNotAffectTheScaling()
    {
        Map<ScalingStat, Double> weights = new HashMap<>();
        weights.put(STRENGTH, 1.0);
        Scaling scaling = new Scaling(weights);

        weights.put(AGILITY, 5.0);

        assertEquals(8.0, scaling.valueFor(stats));
    }

    @Test void theWeightsCannotBeModifiedFromOutside()
    {
        Scaling scaling = Scaling.of(STRENGTH);

        assertThrows(UnsupportedOperationException.class, () -> scaling.weights().put(AGILITY, 1.0));
    }
}