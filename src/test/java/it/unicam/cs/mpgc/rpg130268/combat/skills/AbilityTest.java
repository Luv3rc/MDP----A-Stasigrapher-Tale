package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbility;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.CutsForInk;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.StasigraphicFury;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.ThirstForInk;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbilityTest {
    // The context applies the damage at once and never crits (see FakeCombatContext)
    private final FakeCombatContext context = new FakeCombatContext();

    private Gunther gunther(int startingAp) {
        return new Gunther(new Stats(50, 8, 4, 6, 3), 9, startingAp,
                ApRules.defaults(), InkRules.defaults());
    }

    // Defense 1: the defense lets 30/31 of the power through
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


    // ---------- The context is required ----------

    @Test
    void performWithoutAContextThrowsAndCostsNothing() {
        Gunther g = gunther(3);

        assertThrows(NullPointerException.class, () -> new ThirstForInk(g).perform(List.of(g), null));
        assertEquals(3, g.getAp().getCurrentAp());
        assertEquals(50, g.getHp());
    }


    // ---------- Thirst for Ink (SELF) ----------

    @Test
    void thirstForInkWorksWithExactlyOneAp() {
        Gunther g = gunther(1);
        new ThirstForInk(g).perform(List.of(g), context);

        assertEquals(1, g.getHp());
        assertEquals(9, g.getAp().getCurrentAp());   // paid 1, then refilled to max
        assertEquals(8, g.getInk().getCurrentInk()); // gauge full
        assertEquals(0, context.hitsResolved);       // it asks nothing to the engine
    }

    @Test
    void thirstForInkNeedsOneAp() {
        assertFalse(new ThirstForInk(gunther(0)).canPerform());
    }

    @Test
    void thirstForInkOnAnotherTargetThrowsAndCostsNothing() {
        Gunther g = gunther(3);
        Enemy target = dummy();

        assertThrows(IllegalArgumentException.class, () -> new ThirstForInk(g).perform(List.of(target), context));
        assertEquals(3, g.getAp().getCurrentAp());
        assertEquals(50, g.getHp());
    }

    @Test
    void selfAbilityRejectsAListWithMoreThanTheUser() {
        Gunther g = gunther(3);

        assertThrows(IllegalArgumentException.class,
                () -> new ThirstForInk(g).perform(List.of(g, dummy()), context));
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

        new StasigraphicFury(g).perform(List.of(target), context);

        assertEquals(0, g.getAp().getCurrentAp());   // paid exactly 4
        assertEquals(0, g.getInk().getCurrentInk()); // gauge drained
        assertEquals(4, context.hitsResolved);       // one resolved hit for every hit of the combo

        // Multiplicative Math: Unarmed (Base 1, Str D 0.5)
        // Bonus 1.4, Defense lets 30/31 through.
        // Result is exactly 8 damage across 4 hits (2 + 2 + 2 + 2)
        assertEquals(92, target.getHp());
    }

    @Test
    void furyDealMoreDamageWithMoreInk() {
        Gunther low = gunther(4);
        low.getInk().gainInk(4);
        Enemy lowTarget = dummy();
        new StasigraphicFury(low).perform(List.of(lowTarget), context);

        Gunther high = gunther(4);
        high.getInk().gainInk(8);
        Enemy highTarget = dummy();
        new StasigraphicFury(high).perform(List.of(highTarget), context);

        assertTrue(highTarget.getHp() < lowTarget.getHp());
    }

    @Test
    void furyStopsHittingOnceTheTargetIsDead() {
        Gunther g = gunther(4);
        g.getInk().gainInk(4);
        Enemy weak = new Enemy("Weak", new Stats(5, 1, 1, 1, 1));
        weak.setHp(1); // Dies instantly on first hit

        new StasigraphicFury(g).perform(List.of(weak), context);

        assertFalse(weak.isAlive());
        assertEquals(1, context.hitsResolved);       // the first hit killed it, the other 3 never happened
    }

    @Test
    void refusedFuryCostNothing() {
        Gunther g = gunther(4);
        g.getInk().gainInk(2);                       // not enough ink
        Enemy target = dummy();

        assertThrows(IllegalStateException.class, () -> new StasigraphicFury(g).perform(List.of(target), context));
        assertEquals(4, g.getAp().getCurrentAp());   // AP untouched
        assertEquals(2, g.getInk().getCurrentInk()); // ink untouched
        assertEquals(100, target.getHp());
    }

    @Test
    void furyOnSelfThrowAndCostNothing() {
        Gunther g = gunther(4);
        g.getInk().gainInk(4);

        assertThrows(IllegalArgumentException.class, () -> new StasigraphicFury(g).perform(List.of(g), context));
        assertEquals(4, g.getAp().getCurrentAp());
        assertEquals(4, g.getInk().getCurrentInk());
        assertEquals(50, g.getHp());
    }

    @Test
    void oneEnemyAbilityNeedsExactlyOneTarget() {
        Gunther g = gunther(4);
        g.getInk().gainInk(4);

        assertThrows(IllegalArgumentException.class,
                () -> new StasigraphicFury(g).perform(List.of(dummy(), dummy()), context));
        assertThrows(IllegalArgumentException.class,
                () -> new StasigraphicFury(g).perform(List.of(), context));
        assertEquals(4, g.getAp().getCurrentAp());   // nothing was paid
    }


    // ---------- Cuts for Ink (ONE_ENEMY) ----------

    @Test
    void cutsHitOneTargetAndGiveInkPerHit() {
        Gunther g = gunther(3);
        Enemy target = dummy();

        new CutsForInk(g).perform(List.of(target), context);

        assertEquals(0, g.getAp().getCurrentAp());
        assertEquals(3, context.hitsResolved);

        // Multiplicative Math: Unarmed (Base 1, Str D 0.5)
        // Defense lets 30/31 through.
        // Result is exactly 4 damage across 3 hits (1 + 1 + 2)
        assertEquals(96, target.getHp());
        assertEquals(3 * g.getInkRules().inkGainPerHit(), g.getInk().getCurrentInk());
    }

    @Test
    void cutsStopAndGiveNoMoreInkOnceTheTargetIsDead() {
        Gunther g = gunther(3);
        Enemy weak = new Enemy("Weak", new Stats(5, 1, 1, 1, 1));
        weak.setHp(1); // Dies instantly on first hit

        new CutsForInk(g).perform(List.of(weak), context);

        assertFalse(weak.isAlive());
        assertEquals(1, context.hitsResolved);       // only the first hit happened
        assertEquals(g.getInkRules().inkGainPerHit(), g.getInk().getCurrentInk());   // ink only for that hit
    }


    // ---------- The engine receives the timing of every hit ----------

    @Test
    void cutsHandOverEveryHitWithItsTiming() {
        new CutsForInk(gunther(3)).perform(List.of(dummy()), context);

        // The hits land 400, 600 and 1200 ms after the start (the running sum of the wind-ups)
        assertEquals(List.of(400L, 200L, 600L), context.hitsSeen.stream().map(Hit::windUpMs).toList());
        assertEquals(List.of(StrikeWeight.LIGHT, StrikeWeight.LIGHT, StrikeWeight.NORMAL),
                context.hitsSeen.stream().map(Hit::strikeWeight).toList());
        assertEquals(List.of(1.0, 1.0, 1.0), context.bonusesSeen);   // Cuts has no bonus
    }

    @Test
    void furyHandsOverEveryHitWithItsTimingAndTheInkBonus() {
        Gunther g = gunther(4);
        g.getInk().gainInk(4);

        new StasigraphicFury(g).perform(List.of(dummy()), context);

        assertEquals(List.of(200L, 200L, 200L, 500L), context.hitsSeen.stream().map(Hit::windUpMs).toList());

        double inkBonus = 1 + 4 / 10.0;
        assertEquals(List.of(inkBonus, inkBonus, inkBonus, inkBonus), context.bonusesSeen);
    }

    @Test
    void aDeadTargetIsNotHandedAnyMoreHits() {
        Gunther g = gunther(3);
        Enemy weak = new Enemy("Weak", new Stats(5, 1, 1, 1, 1));
        weak.setHp(1); // Dies instantly

        new CutsForInk(g).perform(List.of(weak), context);

        assertEquals(1, context.hitsSeen.size());    // the combo stopped after the first hit
    }


    // ---------- ALL_ENEMIES (no real skill uses it yet, so a tiny test ability) ----------

    // Costs 1 AP and "hits" every target for 5
    private static class Sweep extends PartyAbility<Gunther> {
        Sweep(Gunther user) {
            super(user, "Sweep", "", TargetType.ALL_ENEMIES);
        }

        @Override
        public int getApCost() {
            return 1;
        }

        @Override
        protected void applyEffect(List<Fighter> targets, CombatContext context) {
            for (Fighter target : targets) target.takeDamage(5);
        }
    }

    @Test
    void allEnemiesAbilityHitsEveryTargetInTheList() {
        Gunther g = gunther(2);
        Enemy first = dummy();
        Enemy second = dummy();

        new AbilityTest.Sweep(g).perform(List.of(first, second), context);

        assertEquals(95, first.getHp());
        assertEquals(95, second.getHp());
        assertEquals(1, g.getAp().getCurrentAp());
    }

    @Test
    void allEnemiesAbilityCannotIncludeItsUser() {
        Gunther g = gunther(2);

        assertThrows(IllegalArgumentException.class, () -> new AbilityTest.Sweep(g).perform(List.of(dummy(), g), context));
        assertEquals(2, g.getAp().getCurrentAp());
    }

    @Test
    void allEnemiesAbilityNeedsAtLeastOneTarget() {
        Gunther g = gunther(2);

        assertThrows(IllegalArgumentException.class, () -> new AbilityTest.Sweep(g).perform(List.of(), context));
    }

    @Test
    void aDeadFighterCannotUseAbilities() {
        Gunther g = gunther(3);
        g.setHp(0);

        assertFalse(new ThirstForInk(g).canPerform());
        assertThrows(IllegalStateException.class, () -> new ThirstForInk(g).perform(List.of(g), context));
        assertEquals(3, g.getAp().getCurrentAp());
    }
}