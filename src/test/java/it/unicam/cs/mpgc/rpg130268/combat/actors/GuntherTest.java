package it.unicam.cs.mpgc.rpg130268.combat.actors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ResourceGauge;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuntherTest {
    private Gunther gunther() {
        return new Gunther(new Stats(50, 8, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults());
    }

    @Test
    void basicAttackGivesApAndInk() {
        Gunther g = gunther();
        g.onBasicAttackLanded();
        assertEquals(4, g.getAp().getCurrentAp());
        assertEquals(1, g.getInk().getCurrentInk());
    }

    @Test
    void battleStartResetsApAndInk() {
        Gunther g = gunther();
        g.getInk().fillInk();
        g.getAp().spendAp(2);
        g.onBattleStart();
        assertEquals(3, g.getAp().getCurrentAp());
        assertEquals(0, g.getInk().getCurrentInk());
    }

    @Test
    void guntherShowsHisInkAsTheExtraGauge() {
        Gunther g = gunther();
        g.getInk().gainInk(3);

        ResourceGauge gauge = g.getExtraGauge().orElseThrow();
        assertEquals("INK", gauge.label());
        assertEquals(3, gauge.current());
        assertEquals(g.getInk().getMaxInk(), gauge.max());
    }

    @Test
    void aFighterWithoutAResourceHasNoExtraGauge() {
        assertTrue(new Enemy("Goblin", new Stats(30, 5, 3, 3, 3)).getExtraGauge().isEmpty());
    }
}
