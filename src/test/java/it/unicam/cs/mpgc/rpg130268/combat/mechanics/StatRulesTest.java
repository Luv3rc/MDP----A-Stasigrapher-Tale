package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatRulesTest
{
    @Test void withNoPointsAMemberHasHisBaseHealthAndNothingElse()
    {
        assertEquals(new Stats(164, 0, 0, 0, 0), StatRules.of(164, 15).statsFor(StatPoints.NONE));
    }

    @Test void everyPointInHealthGivesTheHealthPerPointOfThatMember()
    {
        StatPoints twoInHealth = new StatPoints(2, 0, 0, 0, 0);

        assertEquals(194, StatRules.of(164, 15).statsFor(twoInHealth).maxHealth());
        assertEquals(136, StatRules.of(120, 8).statsFor(twoInHealth).maxHealth());     // another member, other numbers
    }

    @Test void theOtherStatsAreJustTheirPoints()
    {
        assertEquals(new Stats(164, 5, 4, 3, 2), StatRules.of(164, 15).statsFor(new StatPoints(0, 5, 4, 3, 2)));
    }

    @Test void theUsualRulesGiveThreePointsPerLevelAndTheCapOfTheGame()
    {
        StatRules rules = StatRules.of(164, 15);

        assertEquals(3, rules.pointsPerLevel());
        assertEquals(60, rules.statCap());
    }

    @Test void readyMadeStatsComeBackExactlyTheSame()
    {
        Stats stats = new Stats(184, 8, 4, 10, 3);

        assertEquals(stats, StatRules.around(stats).statsFor(StatPoints.of(stats)));
    }

    @Test void badRulesAreRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> new StatRules(0, 15, 3, 60));
        assertThrows(IllegalArgumentException.class, () -> new StatRules(164, -1, 3, 60));
        assertThrows(IllegalArgumentException.class, () -> new StatRules(164, 15, -1, 60));
        assertThrows(IllegalArgumentException.class, () -> new StatRules(164, 15, 3, 0));
    }
}
