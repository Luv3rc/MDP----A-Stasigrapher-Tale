package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.skills.gunther.CutsForInk;
import it.unicam.cs.mpgc.rpg130268.combat.skills.gunther.StasigraphicFury;
import it.unicam.cs.mpgc.rpg130268.combat.skills.gunther.ThirstForInk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbilityTest {
    private Gunther gunther(int startingAp) {
        return new Gunther(new Stats(50, 8, 4, 6, 3), 9, startingAp,
                ApRules.defaults(), InkRules.defaults());
    }

    private Enemy dummy() {
        return new Enemy("Dummy", new Stats(100, 1, 1, 1, 1));
    }

    // ---------- Declared target types ----------

    @Test
    void abilitiesDeclareWhoTheyTarget() {
        Gunther g = gunther(4);
        assertEquals(TargetType.SELF, new ThirstForInk(g).getTargetType());
        assertEquals(TargetType.ONE_ENEMY, new StasigraphicFury(g).getTargetType());
        assertEquals(TargetType.ONE_ENEMY, new CutsForInk(g).getTargetType());
    }

    // ---------- Thirst for Ink (SELF) ----------

    @Test
    void thirstForInkWorksWithExactlyOneAp() {
        Gunther g = gunther(1);
        new ThirstForInk(g).perform(List.of(g));

        assertEquals(1, g.getHp());
        assertEquals(9, g.getAp().getCurrentAp());   // paid 1, then refilled to max
        assertEquals(8, g.getInk().getCurrentInk()); // gauge full
    }

    @Test
    void thirstForInkNeedsOneAp() {
        assertFalse(new ThirstForInk(gunther(0)).canPerform());
    }

    @Test
    void thirstForInkOnAnotherTargetThrowsAndCostsNothing() {
        Gunther g = gunther(3);
        Enemy target = dummy();

        assertThrows(IllegalArgumentException.class, () -> new ThirstForInk(g).perform(List.of(target)));
        assertEquals(3, g.getAp().getCurrentAp());
        assertEquals(50, g.getHp());
    }

    @Test
    void selfAbilityRejectsAListWithMoreThanTheUser() {
        Gunther g = gunther(3);

        assertThrows(IllegalArgumentException.class,
                () -> new ThirstForInk(g).perform(List.of(g, dummy())));
        assertEquals(3, g.getAp().getCurrentAp());
    }

    // ---------- Stasigraphic Fury (ONE_ENEMY) ----------

    @Test
    void furyNeedEnoughInk() {
        Gunther g = gunther(4);
        g.getInk().gainInk(3);                       // 3 < 4
        assertFalse(new StasigraphicFury(g).canPerform());
    }

    @Test
    void furyNeedEnoughAp() {
        Gunther g = gunther(3);                      // 3 < 4
        g.getInk().gainInk(8);
        assertFalse(new StasigraphicFury(g).canPerform());
    }

    @Test
    void furyWorkWithExactApAndInk() {
        Gunther g = gunther(4);
        g.getInk().gainInk(4);
        Enemy target = dummy();

        new StasigraphicFury(g).perform(List.of(target));

        assertEquals(0, g.getAp().getCurrentAp());   // paid exactly 4
        assertEquals(0, g.getInk().getCurrentInk()); // gauge drained
        assertEquals(100 - 33, target.getHp());      // 4 hits * 6 agility * 1.4 = 33.6 -> 33
    }

    @Test
    void furyDealMoreDamageWithMoreInk() {
        Gunther low = gunther(4);
        low.getInk().gainInk(4);
        Enemy lowTarget = dummy();
        new StasigraphicFury(low).perform(List.of(lowTarget));

        Gunther high = gunther(4);
        high.getInk().gainInk(8);
        Enemy highTarget = dummy();
        new StasigraphicFury(high).perform(List.of(highTarget));

        assertTrue(highTarget.getHp() < lowTarget.getHp());
    }

    @Test
    void refusedFuryCostNothing() {
        Gunther g = gunther(4);
        g.getInk().gainInk(2);                       // not enough ink
        Enemy target = dummy();

        assertThrows(IllegalStateException.class, () -> new StasigraphicFury(g).perform(List.of(target)));
        assertEquals(4, g.getAp().getCurrentAp());   // AP untouched
        assertEquals(2, g.getInk().getCurrentInk()); // ink untouched
        assertEquals(100, target.getHp());
    }

    @Test
    void furyOnSelfThrowAndCostNothing() {
        Gunther g = gunther(4);
        g.getInk().gainInk(4);

        assertThrows(IllegalArgumentException.class, () -> new StasigraphicFury(g).perform(List.of(g)));
        assertEquals(4, g.getAp().getCurrentAp());
        assertEquals(4, g.getInk().getCurrentInk());
        assertEquals(50, g.getHp());
    }

    @Test
    void oneEnemyAbilityNeedsExactlyOneTarget() {
        Gunther g = gunther(4);
        g.getInk().gainInk(4);

        assertThrows(IllegalArgumentException.class,
                () -> new StasigraphicFury(g).perform(List.of(dummy(), dummy())));
        assertThrows(IllegalArgumentException.class,
                () -> new StasigraphicFury(g).perform(List.of()));
        assertEquals(4, g.getAp().getCurrentAp());   // nothing was paid
    }

    // ---------- Cuts for Ink (ONE_ENEMY) ----------

    @Test
    void cutsHitOneTargetAndGiveInkPerHit() {
        Gunther g = gunther(3);
        Enemy target = dummy();

        new CutsForInk(g).perform(List.of(target));

        assertEquals(0, g.getAp().getCurrentAp());
        assertEquals(100 - 3 * 8, target.getHp());   // 3 hits * strength 8
        assertEquals(3 * g.getInkRules().inkGainPerHit(), g.getInk().getCurrentInk());
    }

    // ---------- ALL_ENEMIES (no real skill uses it yet, so a tiny test ability) ----------

    // Costs 1 AP and "hits" every target for 5
    private static class Sweep extends PartyAbility<Gunther> {
        Sweep(Gunther user) {
            super(user);
        }

        public String getName() {
            return "Sweep";
        }

        public int getApCost() {
            return 1;
        }

        public TargetType getTargetType() {
            return TargetType.ALL_ENEMIES;
        }

        @Override
        protected void applyEffect(List<Fighter> targets) {
            for (Fighter target : targets) target.takeDamage(5);
        }
    }

    @Test
    void allEnemiesAbilityHitsEveryTargetInTheList() {
        Gunther g = gunther(2);
        Enemy first = dummy();
        Enemy second = dummy();

        new AbilityTest.Sweep(g).perform(List.of(first, second));

        assertEquals(95, first.getHp());
        assertEquals(95, second.getHp());
        assertEquals(1, g.getAp().getCurrentAp());
    }

    @Test
    void allEnemiesAbilityCannotIncludeItsUser() {
        Gunther g = gunther(2);

        assertThrows(IllegalArgumentException.class, () -> new AbilityTest.Sweep(g).perform(List.of(dummy(), g)));
        assertEquals(2, g.getAp().getCurrentAp());
    }

    @Test
    void allEnemiesAbilityNeedsAtLeastOneTarget() {
        Gunther g = gunther(2);

        assertThrows(IllegalArgumentException.class, () -> new AbilityTest.Sweep(g).perform(List.of()));
    }
}
