package it.unicam.cs.mpgc.rpg130268.combat.actors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.CutsForInk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PartyMemberStatPointsTest
{
    private static Gunther fresh()
    {
        return new Gunther(StatRules.of(164, 15), StatPoints.NONE, 0, 9, 3, ApRules.defaults(), InkRules.defaults(), 1, 0);
    }

    @Test void guntherStartsAtLevelOneWithNoPointsAndHisBaseHealth()
    {
        Gunther gunther = fresh();

        assertEquals(new Stats(164, 0, 0, 0, 0), gunther.getStats());
        assertEquals(164, gunther.getHp());
        assertEquals(0, gunther.getUnspentPoints());
        assertEquals(StatPoints.NONE, gunther.getStatPoints());
    }

    @Test void aGuntherOfTheGameKnowsNothingUntilHeLearns()
    {
        Gunther gunther = fresh();

        assertTrue(gunther.getListOfAbility().isEmpty());

        gunther.learn(CutsForInk::new);
        gunther.learn(CutsForInk::new);              // the second time nothing happens

        assertEquals(1, gunther.getListOfAbility().size());
        assertEquals("gunther", gunther.getId());
    }

    @Test void everyLevelGivesPointsToSpend()
    {
        Gunther gunther = fresh();

        gunther.gainXp(200);                      // level 1 -> 2
        assertEquals(2, gunther.getLevel());
        assertEquals(3, gunther.getUnspentPoints());

        gunther.gainXp(500);                      // 2 -> 3 (300) and then not enough for the next one
        assertEquals(3, gunther.getLevel());
        assertEquals(6, gunther.getUnspentPoints());
    }

    @Test void severalLevelsAtOnceGiveThePointsOfEachOne()
    {
        Gunther gunther = fresh();

        gunther.gainXp(200 + 300 + 400);          // 1 -> 4
        assertEquals(4, gunther.getLevel());
        assertEquals(9, gunther.getUnspentPoints());
    }

    @Test void thePointsPerLevelBelongToTheRulesOfTheMember()
    {
        Gunther gunther = new Gunther(new StatRules(164, 15, 5, 60), StatPoints.NONE, 0, 9, 3,
                ApRules.defaults(), InkRules.defaults(), 1, 0);

        gunther.gainXp(200);
        assertEquals(5, gunther.getUnspentPoints());
    }

    @Test void spendingPointsRaisesTheStatAndUsesThePoints()
    {
        Gunther gunther = fresh();
        gunther.gainXp(200);

        gunther.spendPoints(ScalingStat.STRENGTH, 2);

        assertEquals(2, gunther.getStats().strength());
        assertEquals(2, gunther.getStatPoints().strength());
        assertEquals(1, gunther.getUnspentPoints());
    }

    @Test void aPointInHealthRaisesTheMaxHpAndTheCurrentHpFollows()
    {
        Gunther gunther = fresh();
        gunther.gainXp(200);

        gunther.spendPoints(ScalingStat.MAX_HEALTH, 1);

        assertEquals(179, gunther.getStats().maxHealth());
        assertEquals(179, gunther.getHp());                 // he was at full HP and still is
    }

    @Test void aWoundedMemberGainsTheSameHpAsTheMax()
    {
        Gunther gunther = fresh();
        gunther.gainXp(200);
        gunther.setHp(100);

        gunther.spendPoints(ScalingStat.MAX_HEALTH, 2);

        assertEquals(194, gunther.getStats().maxHealth());
        assertEquals(130, gunther.getHp());                 // +30, not a full heal
    }

    @Test void aMemberWhoIsDownStaysDown()
    {
        Gunther gunther = fresh();
        gunther.gainXp(200);
        gunther.setHp(0);

        gunther.spendPoints(ScalingStat.MAX_HEALTH, 1);

        assertEquals(0, gunther.getHp());
        assertFalse(gunther.isAlive());
    }

    @Test void youCannotSpendPointsYouDoNotHave()
    {
        Gunther gunther = fresh();
        gunther.gainXp(200);

        assertThrows(IllegalStateException.class, () -> gunther.spendPoints(ScalingStat.LUCK, 4));
        assertThrows(IllegalArgumentException.class, () -> gunther.spendPoints(ScalingStat.LUCK, 0));
        assertThrows(NullPointerException.class, () -> gunther.spendPoints(null, 1));
        assertEquals(3, gunther.getUnspentPoints());
        assertEquals(StatPoints.NONE, gunther.getStatPoints());
    }

    @Test void aStatCannotGoOverTheCapOfTheMember()
    {
        Gunther gunther = new Gunther(new StatRules(164, 15, 3, 2), StatPoints.NONE, 3, 9, 3,
                ApRules.defaults(), InkRules.defaults(), 1, 0);

        assertThrows(IllegalArgumentException.class, () -> gunther.spendPoints(ScalingStat.STRENGTH, 3));

        gunther.spendPoints(ScalingStat.STRENGTH, 2);
        assertFalse(gunther.canIncrease(ScalingStat.STRENGTH));        // at the cap
        assertTrue(gunther.canIncrease(ScalingStat.AGILITY));          // one more point to spend elsewhere
    }

    @Test void withNothingToSpendNoStatCanIncrease()
    {
        Gunther gunther = fresh();

        for (ScalingStat stat : ScalingStat.values())
            assertFalse(gunther.canIncrease(stat));
    }

    @Test void aMemberBroughtBackFromASaveKeepsHisPointsAndHisStats()
    {
        Gunther gunther = new Gunther(StatRules.of(164, 15), new StatPoints(2, 4, 0, 6, 1), 3, 9, 3,
                ApRules.defaults(), InkRules.defaults(), 5, 120);

        assertEquals(new Stats(194, 4, 0, 6, 1), gunther.getStats());
        assertEquals(5, gunther.getLevel());
        assertEquals(120, gunther.getCurrentXp());
        assertEquals(3, gunther.getUnspentPoints());
    }

    @Test void aMemberMadeFromReadyMadeStatsKeepsExactlyThoseStats()
    {
        Gunther gunther = new Gunther(new Stats(184, 8, 4, 10, 3), 9, 3, ApRules.defaults(), InkRules.defaults());

        assertEquals(new Stats(184, 8, 4, 10, 3), gunther.getStats());
        assertEquals(new StatPoints(0, 8, 4, 10, 3), gunther.getStatPoints());
        assertEquals(0, gunther.getUnspentPoints());
    }

    @Test void badStartingValuesAreRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> new Gunther(StatRules.of(164, 15), StatPoints.NONE, -1, 9, 3,
                ApRules.defaults(), InkRules.defaults(), 1, 0));
        assertThrows(NullPointerException.class, () -> new Gunther(null, StatPoints.NONE, 0, 9, 3,
                ApRules.defaults(), InkRules.defaults(), 1, 0));
    }
}
