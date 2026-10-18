package it.unicam.cs.mpgc.rpg130268.combat.actors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
