package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleRulesTest
{
    @Test void defaultCounterIsAHeavyStrike()
    {
        assertEquals(StrikeWeight.HEAVY, BattleRules.defaults().counterWeight());
    }

    @Test void customCounterWeightIsKept()
    {
        assertEquals(StrikeWeight.LIGHT, new BattleRules(StrikeWeight.LIGHT).counterWeight());
    }

    @Test void byDefaultThereIsNoDeadZoneAndAnnouncementsLastTheUsualTime()
    {
        assertEquals(0, BattleRules.defaults().deadZoneMs());
        assertEquals(BattleRules.DEFAULT_ANNOUNCEMENT_MS, BattleRules.defaults().announcementMs());
    }

    @Test void deadZoneAndAnnouncementTimeAreKept()
    {
        BattleRules rules = new BattleRules(StrikeWeight.LIGHT, 800, 2000);

        assertEquals(800, rules.deadZoneMs());
        assertEquals(2000, rules.announcementMs());
    }

    @Test void timesCannotBeNegative()
    {
        assertThrows(IllegalArgumentException.class, () -> new BattleRules(StrikeWeight.LIGHT, -1, 1000));
        assertThrows(IllegalArgumentException.class, () -> new BattleRules(StrikeWeight.LIGHT, 0, -1));
    }

    @Test void rulesNeedAWeight()
    {
        assertThrows(NullPointerException.class, () -> new BattleRules(null));
    }
}