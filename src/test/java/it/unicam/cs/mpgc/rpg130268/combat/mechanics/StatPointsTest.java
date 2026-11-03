package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatPointsTest
{
    @Test void noneHasNoPoints()
    {
        assertEquals(0, StatPoints.NONE.total());

        for (ScalingStat stat : ScalingStat.values())
            assertEquals(0, StatPoints.NONE.get(stat));
    }

    @Test void plusPutsThePointsInTheRightStatAndLeavesTheOriginalAlone()
    {
        StatPoints start = StatPoints.NONE;

        assertEquals(new StatPoints(2, 0, 0, 0, 0), start.plus(ScalingStat.MAX_HEALTH, 2));
        assertEquals(new StatPoints(0, 2, 0, 0, 0), start.plus(ScalingStat.STRENGTH, 2));
        assertEquals(new StatPoints(0, 0, 2, 0, 0), start.plus(ScalingStat.DEFENSE, 2));
        assertEquals(new StatPoints(0, 0, 0, 2, 0), start.plus(ScalingStat.AGILITY, 2));
        assertEquals(new StatPoints(0, 0, 0, 0, 2), start.plus(ScalingStat.LUCK, 2));
        assertEquals(StatPoints.NONE, start);
    }

    @Test void getAnswersForEveryStat()
    {
        StatPoints points = new StatPoints(1, 2, 3, 4, 5);

        assertEquals(1, points.get(ScalingStat.MAX_HEALTH));
        assertEquals(2, points.get(ScalingStat.STRENGTH));
        assertEquals(3, points.get(ScalingStat.DEFENSE));
        assertEquals(4, points.get(ScalingStat.AGILITY));
        assertEquals(5, points.get(ScalingStat.LUCK));
        assertEquals(15, points.total());
    }

    @Test void ReadyMadeStatsBecomePointsWithNoneInHealth()
    {
        assertEquals(new StatPoints(0, 8, 4, 10, 3), StatPoints.of(new Stats(184, 8, 4, 10, 3)));
    }

    @Test void negativePointsAndEmptyAdditionsAreRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> new StatPoints(-1, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new StatPoints(0, 0, 0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> StatPoints.NONE.plus(ScalingStat.LUCK, 0));
        assertThrows(NullPointerException.class, () -> StatPoints.NONE.plus(null, 1));
    }
}
