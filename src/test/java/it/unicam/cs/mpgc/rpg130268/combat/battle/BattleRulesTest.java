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

    @Test void rulesNeedAWeight()
    {
        assertThrows(NullPointerException.class, () -> new BattleRules(null));
    }
}