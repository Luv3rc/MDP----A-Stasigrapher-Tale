package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XpRewardsTest
{
    private static Gunther member() { return new Gunther(new Stats(100, 10, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults()); }

    private static Enemy enemy(int xp) { return new Enemy("Goblin", new Stats(10, 5, 2, 3, 0), 1, xp); }

    @Test void theXpOfAllTheEnemiesIsSharedInTheParty()
    {
        Gunther only = member();

        Map<?, Integer> shares = XpRewards.defaults().award(List.of(only), List.of(enemy(30), enemy(20)));

        assertEquals(Map.of(only, 50), shares);
    }

    @Test void theFallenComeBackWithOneHp()
    {
        Gunther standing = member();
        Gunther fallen = member();
        fallen.setHp(0);

        XpRewards.defaults().award(List.of(standing, fallen), List.of(enemy(100)));

        assertTrue(fallen.isAlive());
        assertEquals(1, fallen.getHp());
    }

    @Test void theRulesAreRequired()
    {
        assertThrows(NullPointerException.class, () -> new XpRewards(null));
    }
}
