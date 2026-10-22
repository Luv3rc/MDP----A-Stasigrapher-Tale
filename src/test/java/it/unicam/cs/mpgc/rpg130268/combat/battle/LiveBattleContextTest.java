package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageRules;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOutcome;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionQuality;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class LiveBattleContextTest
{
    // Nobody ever crits (the roll is always 0.99)
    private static class NoCrits implements RandomGenerator
    {
        @Override public double nextDouble() { return 0.99; }
        @Override public long nextLong() { return 0; }
    }

    private final BattleClock clock = new BattleClock();

    private LiveBattleContext live()
    {
        return new LiveBattleContext(new DamageCalculator(DamageRules.defaults(), new NoCrits()),
                ReactionWindows.defaults(), BattleRules.defaults(), clock);
    }

    // strength 10, defense 2: a NORMAL hit of 20 and a HEAVY counter of 30 (the defense lets 30/32 through)
    private Enemy goblin() { return goblin(40); }

    private Enemy goblin(int hp)
    {
        Enemy goblin = new Enemy("Goblin", new Stats(hp, 10, 2, 3, 0));
        goblin.equip(new Weapon("Club", WeaponType.UNARMED, 10, ScalingGrade.NONE));
        return goblin;
    }

    // strength 10, defense 4: the defense lets 30/34 through, so a NORMAL hit of the goblin does 17
    private Gunther gunther()
    {
        Gunther gunther = new Gunther(new Stats(50, 10, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults());
        gunther.equip(new Weapon("Test sword", WeaponType.SWORD, 10, ScalingGrade.NONE));
        return gunther;
    }

    // Default windows: parry perfect from 120 ms before the impact to 40 ms after it,
    // dodge good from 225 ms before to 75 ms after it
    private static Hit hit(long windUp, ReactionOptions options) { return new Hit(windUp, options, StrikeWeight.NORMAL); }
    private static Hit parryable() { return hit(500, ReactionOptions.PARRY_OR_DODGE); }

    private static ReactionOutcome perfectParry() { return new ReactionOutcome(ReactionType.PARRY, ReactionQuality.PERFECT); }

    // ---------- The attack plays out on the clock ----------

    @Test void anAttackIsOnlyScheduledNothingHappensYet()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        Enemy attacker = goblin();

        int total = live.resolveAttack(attacker, List.of(g), List.of(parryable()));

        assertEquals(0, total);                          // the damage is not known yet
        assertEquals(50, g.getHp());
        assertTrue(live.isBusy());
        assertEquals(List.of(new BattleEvent.AttackStarted(attacker, List.of(g), List.of(parryable()), 0)), live.drainEvents());
    }

    @Test void aHitIsJudgedWhenItsReactionWindowCloses()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        live.resolveAttack(goblin(), List.of(g), List.of(parryable()));

        live.update(500);                                // the impact: the player could still react a bit late
        assertEquals(50, g.getHp());

        live.update(74);                                 // 574: the dodge window is still open (75 ms late)
        assertEquals(50, g.getHp());

        live.update(1);                                  // 575: closed!
        assertEquals(50 - 17, g.getHp());
        assertFalse(live.isBusy());
    }

    @Test void theEventsAreStampedWithTheMomentOfTheImpact()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        Enemy attacker = goblin();
        live.resolveAttack(attacker, List.of(g), List.of(parryable()));
        live.drainEvents();

        live.update(600);

        assertEquals(List.of(new BattleEvent.HitResolved(attacker, g, ReactionOutcome.noReaction(), 17, false, 500)),
                live.drainEvents());
    }

    @Test void aHitThatCannotBeReactedToIsJudgedAtTheImpact()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        live.resolveAttack(goblin(), List.of(g), List.of(hit(500, ReactionOptions.NONE)));

        live.update(499);
        assertEquals(50, g.getHp());

        live.update(1);
        assertEquals(50 - 17, g.getHp());
    }

    @Test void aBigStepResolvesEverythingDueInOrder()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        Enemy attacker = goblin();
        live.resolveAttack(attacker, List.of(g), List.of(parryable(), parryable()));
        live.drainEvents();

        live.update(5000);

        assertEquals(50 - 34, g.getHp());
        assertEquals(List.of(500L, 1000L), live.drainEvents().stream().map(BattleEvent::atMs).toList());
        assertFalse(live.isBusy());
    }

    // ---------- The player presses keys on the same clock ----------

    @Test void aPerfectParryAvoidsTheHitAndGivesTheCounter()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        Enemy attacker = goblin();
        int ap = g.getAp().getCurrentAp();
        live.resolveAttack(attacker, List.of(g), List.of(parryable()));
        live.drainEvents();

        live.update(500);
        assertTrue(live.react(ReactionType.PARRY));      // right on the impact
        live.update(87);

        assertEquals(50, g.getHp());
        assertEquals(40 - 28, attacker.getHp());         // the counter: a heavy basic attack
        assertTrue(g.getAp().getCurrentAp() > ap);       // the perfect parry and the counter gave AP
        assertEquals(List.of(
                        new BattleEvent.HitResolved(attacker, g, perfectParry(), 0, false, 500),
                        new BattleEvent.Countered(g, attacker, 28, false, 500)),
                live.drainEvents());
    }

    @Test void aParryAFewMillisecondsLateIsStillPerfect()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        live.resolveAttack(goblin(), List.of(g), List.of(parryable()));

        live.update(540);                                // 40 ms after the impact, the window allows 40
        assertTrue(live.react(ReactionType.PARRY));
        live.update(47);

        assertEquals(50, g.getHp());
    }

    @Test void aParryTooLateCountsForNothing()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        live.resolveAttack(goblin(), List.of(g), List.of(parryable()));

        live.update(560);                                // 60 ms after the impact: too late for a parry
        assertFalse(live.react(ReactionType.PARRY));
        live.update(27);

        assertEquals(50 - 17, g.getHp());
    }

    @Test void aLateDodgeStillWorks()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        live.resolveAttack(goblin(), List.of(g), List.of(parryable()));

        live.update(560);                                // the dodge window allows 75 ms
        assertTrue(live.react(ReactionType.DODGE));
        live.update(27);

        assertEquals(50, g.getHp());
    }

    @Test void aDodgeAvoidsTheHitButGivesNoCounter()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        Enemy attacker = goblin();
        live.resolveAttack(attacker, List.of(g), List.of(parryable()));

        live.update(300);                                // 200 ms early: inside the dodge window only
        assertTrue(live.react(ReactionType.DODGE));
        live.update(287);

        assertEquals(50, g.getHp());
        assertEquals(40, attacker.getHp());
    }

    @Test void onlyTheFirstKeyOfAHitCounts()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        live.resolveAttack(goblin(), List.of(g), List.of(parryable()));

        live.update(5);
        assertTrue(live.react(ReactionType.PARRY));      // far too early: it is taken, and it is a miss
        live.update(495);
        assertFalse(live.react(ReactionType.PARRY));     // the right moment, but this hit already had its key
        live.update(87);

        assertEquals(50 - 17, g.getHp());
    }

    @Test void aKeyThatTheHitDoesNotAcceptIsIgnored()
    {
        LiveBattleContext live = live();
        live.resolveAttack(goblin(), List.of(gunther()), List.of(hit(500, ReactionOptions.PARRY_ONLY)));

        live.update(300);

        assertFalse(live.react(ReactionType.DODGE));
    }

    @Test void thereIsNothingToReactToWithoutAnAttack()
    {
        LiveBattleContext live = live();

        assertFalse(live.react(ReactionType.PARRY));
        assertThrows(IllegalArgumentException.class, () -> live.react(ReactionType.NONE));
        assertThrows(NullPointerException.class, () -> live.react(null));
    }

    // ---------- Several hits ----------

    @Test void everyHitTakesItsOwnKeyAndParryingAllOfThemGivesTheCounter()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        Enemy attacker = goblin();
        live.resolveAttack(attacker, List.of(g), List.of(parryable(), hit(300, ReactionOptions.PARRY_ONLY)));
        live.drainEvents();

        live.update(500);                                // the first impact
        assertTrue(live.react(ReactionType.PARRY));
        live.update(300);                                // the second impact (800): the first one is judged now
        assertTrue(live.react(ReactionType.PARRY));      // and the key goes to the second hit
        live.update(50);

        assertEquals(50, g.getHp());
        assertEquals(40 - 28, attacker.getHp());
        assertEquals(List.of(
                        new BattleEvent.HitResolved(attacker, g, perfectParry(), 0, false, 500),
                        new BattleEvent.HitResolved(attacker, g, perfectParry(), 0, false, 800),
                        new BattleEvent.Countered(g, attacker, 28, false, 800)),
                live.drainEvents());
    }

    @Test void parryingOnlyTheFirstHitGivesNoCounter()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        Enemy attacker = goblin();
        live.resolveAttack(attacker, List.of(g), List.of(parryable(), parryable()));

        live.update(500);
        live.react(ReactionType.PARRY);
        live.update(5000);

        assertEquals(50 - 17, g.getHp());                // only the second hit landed
        assertEquals(40, attacker.getHp());
    }

    // ---------- A hit that eats everyone ----------

    @Test void oneKeyCoversTheWholeParty()
    {
        LiveBattleContext live = live();
        Gunther first = gunther();
        Gunther second = gunther();
        int ap = first.getAp().getCurrentAp();
        live.resolveAttack(goblin(100), List.of(first, second), List.of(parryable()));

        live.update(500);
        live.react(ReactionType.PARRY);
        live.update(87);

        assertEquals(50, first.getHp());
        assertEquals(50, second.getHp());
        assertTrue(first.getAp().getCurrentAp() > ap);
        assertTrue(second.getAp().getCurrentAp() > ap);
    }

    @Test void withoutAKeyEveryoneTakesTheDamage()
    {
        LiveBattleContext live = live();
        Gunther first = gunther();
        Gunther second = gunther();
        live.resolveAttack(goblin(), List.of(first, second), List.of(parryable()));

        live.update(1000);

        assertEquals(50 - 17, first.getHp());
        assertEquals(50 - 17, second.getHp());
    }

    @Test void counterThatDefeatsTheAttackerStopsTheOtherCounters()
    {
        LiveBattleContext live = live();
        Enemy attacker = goblin(20);
        live.resolveAttack(attacker, List.of(gunther(), gunther()), List.of(parryable()));
        live.drainEvents();

        live.update(500);
        live.react(ReactionType.PARRY);
        live.update(87);

        List<BattleEvent> events = live.drainEvents();
        assertFalse(attacker.isAlive());
        assertEquals(1, events.stream().filter(e -> e instanceof BattleEvent.Countered).count());
        assertEquals(1, events.stream().filter(e -> e instanceof BattleEvent.Defeated).count());
    }

    // ---------- Deaths ----------

    @Test void theAttackEndsWhenEveryTargetHasFallen()
    {
        LiveBattleContext live = live();
        Gunther fragile = gunther();
        fragile.setHp(5);
        Enemy attacker = goblin();
        live.resolveAttack(attacker, List.of(fragile), List.of(parryable(), parryable()));
        live.drainEvents();

        live.update(5000);

        assertFalse(live.isBusy());
        assertEquals(List.of(new BattleEvent.HitResolved(attacker, fragile, ReactionOutcome.noReaction(), 17, false, 500),
                new BattleEvent.Defeated(fragile, 500)), live.drainEvents());
    }

    @Test void aDeadAttackerSchedulesNothing()
    {
        LiveBattleContext live = live();
        Enemy dead = goblin();
        dead.setHp(0);

        assertEquals(0, live.resolveAttack(dead, List.of(gunther()), List.of(parryable())));

        assertFalse(live.isBusy());
        assertTrue(live.drainEvents().isEmpty());
    }

    // ---------- One thing at a time, all on the same clock ----------

    @Test void nothingElseCanBeResolvedWhileAnAttackIsInProgress()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        Enemy attacker = goblin();
        live.resolveAttack(attacker, List.of(g), List.of(parryable()));

        assertThrows(IllegalStateException.class, () -> live.resolveAttack(attacker, List.of(g), List.of(parryable())));
        assertThrows(IllegalStateException.class, () -> live.resolveHit(g, attacker, parryable()));
        assertThrows(IllegalStateException.class, () -> live.heal(attacker, attacker, 5));

        live.update(1000);

        assertDoesNotThrow(() -> live.resolveHit(g, attacker, parryable()));
    }

    @Test void anAttackStartsWhenTheStrikesBeforeItHaveBeenPlayed()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        Enemy attacker = goblin();
        live.resolveHit(g, goblin(), hit(400, ReactionOptions.NONE));      // the player's combo lasts until 400
        live.drainEvents();

        live.resolveAttack(attacker, List.of(g), List.of(parryable()));

        assertEquals(400, live.drainEvents().getFirst().atMs());           // AttackStarted waits for the combo
        assertFalse(live.react(ReactionType.PARRY));                       // nothing to react to yet

        live.update(900);                                                  // the impact is at 400 + 500
        assertTrue(live.react(ReactionType.PARRY));
        live.update(87);

        assertEquals(50, g.getHp());
        assertEquals(900, live.drainEvents().getFirst().atMs());
    }

    @Test void theUiAndTheEngineShareTheSameClock()
    {
        LiveBattleContext live = live();
        live.resolveAttack(goblin(), List.of(gunther()), List.of(parryable()));

        live.update(120);
        clock.advance(30);                                                 // time can also be moved from the outside

        live.update(0);                                                    // and update(0) resolves whatever is due
        assertEquals(150, clock.getNowMs());
    }

    // ---------- Validation ----------

    @Test void anAttackNeedsTargetsAndHits()
    {
        LiveBattleContext live = live();

        assertThrows(IllegalArgumentException.class, () -> live.resolveAttack(goblin(), List.of(), List.of(parryable())));
        assertThrows(IllegalArgumentException.class, () -> live.resolveAttack(goblin(), List.of(gunther()), List.of()));
        assertThrows(NullPointerException.class, () -> live.resolveAttack(null, List.of(gunther()), List.of(parryable())));
        assertThrows(NullPointerException.class, () -> new LiveBattleContext(null));
    }

    // ---------- Idle: everything scheduled has been played ----------

    @Test void theEngineIsIdleOnlyWhenEverythingScheduledHasBeenPlayed()
    {
        LiveBattleContext live = live();
        assertTrue(live.isIdle());

        live.resolveHit(gunther(), goblin(), hit(400, ReactionOptions.NONE));
        assertFalse(live.isIdle());                      // it is resolved, but its replay takes 400 ms
        live.update(399);
        assertFalse(live.isIdle());
        live.update(1);
        assertTrue(live.isIdle());

        live.resolveAttack(goblin(), List.of(gunther()), List.of(parryable()));   // starts at 400, lands at 900
        assertFalse(live.isIdle());
        live.update(574);                                // 974: the reaction window is still open
        assertFalse(live.isIdle());
        live.update(1);                                  // 975: judged
        assertTrue(live.isIdle());
    }

    @Test void aGroupHitCannotBeResolvedWhileAnAttackIsInProgress()
    {
        LiveBattleContext live = live();
        Gunther g = gunther();
        Enemy attacker = goblin();
        live.resolveAttack(attacker, List.of(g), List.of(parryable()));

        assertThrows(IllegalStateException.class, () -> live.resolveHit(g, List.of(attacker), parryable()));
    }

    @Test void theNextDueMomentIsWhenTheNextHitCanBeJudged()
    {
        LiveBattleContext live = live();
        live.resolveAttack(goblin(), List.of(gunther()), List.of(parryable(), hit(300, ReactionOptions.PARRY_ONLY)));

        assertEquals(575, live.nextDueMs());             // the first impact (500) and the late dodge window (75)

        live.update(575);
        assertEquals(840, live.nextDueMs());             // the second impact (800) and the late parry window (40)
    }

    @Test void whenNothingIsHappeningTheNextDueMomentIsTheEndOfWhatWasScheduled()
    {
        LiveBattleContext live = live();

        live.resolveHit(gunther(), goblin(), hit(400, ReactionOptions.NONE));

        assertEquals(400, live.nextDueMs());
    }
}