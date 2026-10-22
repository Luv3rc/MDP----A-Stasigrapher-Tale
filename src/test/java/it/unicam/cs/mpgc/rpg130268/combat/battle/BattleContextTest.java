package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageResult;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageRules;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionInput;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOutcome;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionQuality;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionResolver;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class BattleContextTest
{
    // Nobody ever crits (the roll is always 0.99)
    private static class NoCrits implements RandomGenerator
    {
        @Override public double nextDouble() { return 0.99; }
        @Override public long nextLong() { return 0; }
    }

    // A reaction source that follows a script, one input for every hit that asks
    private static class ScriptedReactions implements ReactionSource
    {
        private final List<ReactionInput> script;
        final List<Hit> asked = new ArrayList<>();
        final List<List<Fighter>> askedTargets = new ArrayList<>();

        ScriptedReactions(ReactionInput... script) { this.script = new ArrayList<>(List.of(script)); }

        @Override
        public ReactionInput inputFor(List<Fighter> targets, Hit hit)
        {
            asked.add(hit);
            askedTargets.add(targets);
            return script.isEmpty() ? ReactionInput.none() : script.removeFirst();
        }
    }

    private BattleContext battle(ReactionSource reactions)
    {
        return battle(reactions, BattleRules.defaults());
    }

    private BattleContext battle(ReactionSource reactions, BattleRules rules)
    {
        return new BattleContext(new DamageCalculator(DamageRules.defaults(), new NoCrits()),
                new ReactionResolver(ReactionWindows.defaults()), reactions, rules);
    }

    // Weapons with no scaling: the damage is base * (1 + strength / 10) * the weight of the hit.
    // With strength 10 and base 10 that is 20 for a NORMAL hit, 30 for a HEAVY one, 15 for a LIGHT one

    // strength 10, defense 2, no luck: never crits. The defense lets 30/32 through
    private Enemy goblin() { return goblin(40); }

    private Enemy goblin(int hp)
    {
        Enemy goblin = new Enemy("Goblin", new Stats(hp, 10, 2, 3, 0));
        goblin.equip(new Weapon("Club", WeaponType.UNARMED, 10, ScalingGrade.NONE));
        return goblin;
    }

    // strength 10, defense 4: the defense lets 30/34 through
    private Gunther gunther()
    {
        Gunther gunther = new Gunther(new Stats(50, 10, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults());
        gunther.equip(new Weapon("Test sword", WeaponType.SWORD, 10, ScalingGrade.NONE));
        return gunther;
    }

    // Default windows: the parry is perfect from 150 ms before the impact to 50 ms after it,
    // a dodge is good from 263 ms before to 87 ms after it. The impact of these hits is at 500 ms
    private static Hit hit(long windUp, ReactionOptions options, StrikeWeight weight) { return new Hit(windUp, options, weight); }
    private static Hit parryable() { return hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL); }

    private static ReactionInput perfectParry() { return new ReactionInput(ReactionType.PARRY, 500); }
    private static ReactionInput goodDodge() { return new ReactionInput(ReactionType.DODGE, 300); }
    private static ReactionInput earlyParry() { return new ReactionInput(ReactionType.PARRY, 100); }

    // ---------- resolveHit: the player's own strikes ----------

    @Test void resolveHitAppliesTheDamageAndReturnsTheResult()
    {
        Gunther g = gunther();
        Enemy target = goblin();

        DamageResult result = battle(ReactionSource.none()).resolveHit(g, target, parryable());

        assertEquals(18, result.amount());               // 20 against defense 2: 18.75 -> 18
        assertEquals(40 - 18, target.getHp());
    }

    @Test void resolveHitNeverAsksForAReaction()
    {
        ScriptedReactions source = new ScriptedReactions();

        battle(source).resolveHit(gunther(), goblin(), parryable());

        assertTrue(source.asked.isEmpty());
    }

    @Test void aComboLandsAtTheRunningSumOfTheWindUps()
    {
        BattleContext battle = battle(ReactionSource.none());
        Gunther g = gunther();
        Enemy target = goblin(500);

        battle.resolveHit(g, target, hit(400, ReactionOptions.NONE, StrikeWeight.LIGHT));
        battle.resolveHit(g, target, hit(200, ReactionOptions.NONE, StrikeWeight.LIGHT));
        battle.resolveHit(g, target, hit(600, ReactionOptions.NONE, StrikeWeight.NORMAL));

        List<Long> times = battle.drainEvents().stream().map(BattleEvent::atMs).toList();
        assertEquals(List.of(400L, 600L, 1200L), times);
    }

    @Test void theBonusMultiplierRaisesTheDamage()
    {
        Gunther g = gunther();

        int plain = battle(ReactionSource.none()).resolveHit(g, goblin(), parryable()).amount();
        int boosted = battle(ReactionSource.none()).resolveHit(g, goblin(), parryable(), 2.0).amount();

        assertEquals(18, plain);
        assertEquals(37, boosted);                       // 40 against defense 2: 37.5 -> 37
    }

    @Test void resolveHitRecordsWhatHappened()
    {
        BattleContext battle = battle(ReactionSource.none());
        Gunther g = gunther();
        Enemy target = goblin();

        battle.resolveHit(g, target, hit(400, ReactionOptions.NONE, StrikeWeight.NORMAL));

        assertEquals(List.of(new BattleEvent.HitResolved(g, target, ReactionOutcome.noReaction(), 18, false, 400)),
                battle.drainEvents());
    }

    // ---------- Events and the clock ----------

    @Test void drainingGivesTheEventsOnlyOnce()
    {
        BattleContext battle = battle(ReactionSource.none());

        battle.resolveHit(gunther(), goblin(), hit(400, ReactionOptions.NONE, StrikeWeight.NORMAL));

        assertEquals(1, battle.drainEvents().size());
        assertTrue(battle.drainEvents().isEmpty());
    }

    @Test void theClockKeepsRunningAcrossActionsAndNeverGoesBack()
    {
        BattleClock clock = new BattleClock();
        BattleContext battle = new BattleContext(new DamageCalculator(DamageRules.defaults(), new NoCrits()),
                new ReactionResolver(ReactionWindows.defaults()), ReactionSource.none(), BattleRules.defaults(), clock);
        Gunther g = gunther();
        Enemy target = goblin(500);

        battle.resolveHit(g, target, hit(400, ReactionOptions.NONE, StrikeWeight.NORMAL));
        assertEquals(400, battle.drainEvents().getFirst().atMs());

        battle.resolveHit(g, target, hit(300, ReactionOptions.NONE, StrikeWeight.NORMAL));
        assertEquals(700, battle.drainEvents().getFirst().atMs());        // it starts where the last one ended

        clock.advance(2000);                                              // the UI let time pass
        battle.resolveHit(g, target, hit(300, ReactionOptions.NONE, StrikeWeight.NORMAL));
        assertEquals(2300, battle.drainEvents().getFirst().atMs());       // it starts now, not in the past
    }

    @Test void theDrainedEventsCannotBeChanged()
    {
        BattleContext battle = battle(ReactionSource.none());
        battle.resolveHit(gunther(), goblin(), parryable());

        assertThrows(UnsupportedOperationException.class, () -> battle.drainEvents().clear());
    }

    // ---------- resolveAttack: reactions ----------

    // ---------- A hit that eats everyone ----------

    @Test void aFailedReactionHurtsEveryone()
    {
        Gunther first = gunther();
        Gunther second = gunther();
        List<Hit> hits = List.of(parryable(), hit(300, ReactionOptions.PARRY_ONLY, StrikeWeight.HEAVY));

        int total = battle(ReactionSource.none()).resolveAttack(goblin(), List.of(first, second), hits);

        // each takes 17 + 26 = 43
        assertEquals(86, total);
        assertEquals(50 - 43, first.getHp());
        assertEquals(50 - 43, second.getHp());
    }

    @Test void everyoneReactsOnceToEveryHitTogether()
    {
        ScriptedReactions source = new ScriptedReactions();
        Gunther first = gunther();
        Gunther second = gunther();
        Hit other = hit(300, ReactionOptions.PARRY_ONLY, StrikeWeight.LIGHT);

        battle(source).resolveAttack(goblin(), List.of(first, second), List.of(parryable(), other));

        assertEquals(List.of(parryable(), other), source.asked);               // one question per hit, not per target
        assertEquals(List.of(List.of(first, second), List.of(first, second)), source.askedTargets);
    }

    @Test void aPerfectParryCoversEveryone()
    {
        Gunther first = gunther();
        Gunther second = gunther();
        int ap = first.getAp().getCurrentAp();

        int total = battle(new ScriptedReactions(perfectParry()))
                .resolveAttack(goblin(500), List.of(first, second), List.of(parryable(), parryable()));

        // the first hit is parried by both, the second one (nobody reacts) eats both
        assertEquals(34, total);
        assertEquals(50 - 17, first.getHp());
        assertEquals(50 - 17, second.getHp());
        assertEquals(ap + ApRules.defaults().apGainOnPerfectParry(), first.getAp().getCurrentAp());
        assertEquals(ap + ApRules.defaults().apGainOnPerfectParry(), second.getAp().getCurrentAp());
    }

    @Test void ifTheWholePartyParriesEveryHitEveryoneCounters()
    {
        Gunther first = gunther();
        Gunther second = gunther();
        Enemy attacker = goblin(100);
        BattleContext battle = battle(new ScriptedReactions(perfectParry(), perfectParry()));

        int total = battle.resolveAttack(attacker, List.of(first, second), List.of(parryable(), parryable()));

        assertEquals(0, total);
        assertEquals(50, first.getHp());
        assertEquals(50, second.getHp());
        assertEquals(100 - 28 - 28, attacker.getHp());   // two counters of 28

        List<BattleEvent> events = battle.drainEvents();
        assertEquals(List.of(new BattleEvent.Countered(first, attacker, 28, false, 1000),
                        new BattleEvent.Countered(second, attacker, 28, false, 1000)),
                events.subList(events.size() - 2, events.size()));
    }

    @Test void aCounterThatDefeatsTheAttackerStopsTheOtherCounters()
    {
        Enemy attacker = goblin(20);
        BattleContext battle = battle(new ScriptedReactions(perfectParry()));

        battle.resolveAttack(attacker, List.of(gunther(), gunther()), List.of(parryable()));

        assertFalse(attacker.isAlive());
        List<BattleEvent> events = battle.drainEvents();
        assertEquals(1, events.stream().filter(e -> e instanceof BattleEvent.Countered).count());
        assertEquals(1, events.stream().filter(e -> e instanceof BattleEvent.Defeated).count());
    }

    @Test void everyoneIsHitAtTheSameMoment()
    {
        BattleContext battle = battle(ReactionSource.none());

        battle.resolveAttack(goblin(), List.of(gunther(), gunther()),
                List.of(parryable(), hit(300, ReactionOptions.PARRY_ONLY, StrikeWeight.NORMAL)));

        assertEquals(List.of(500L, 500L, 800L, 800L), battle.drainEvents().stream().map(BattleEvent::atMs).toList());
    }

    @Test void aFallenTargetLeavesTheAttackAndTheOthersKeepBeingHit()
    {
        ScriptedReactions source = new ScriptedReactions();
        Gunther fragile = gunther();
        fragile.setHp(5);
        Gunther sturdy = gunther();
        BattleContext battle = battle(source);

        battle.resolveAttack(goblin(500), List.of(fragile, sturdy), List.of(parryable(), parryable()));

        assertFalse(fragile.isAlive());
        assertEquals(50 - 34, sturdy.getHp());                       // both hits
        assertEquals(List.of(fragile, sturdy), source.askedTargets.get(0));
        assertEquals(List.of(sturdy), source.askedTargets.get(1));   // the second hit only eats who is left
        assertTrue(battle.drainEvents().contains(new BattleEvent.Defeated(fragile, 500)));
    }

    @Test void deadTargetsAreNotPartOfTheAttack()
    {
        ScriptedReactions source = new ScriptedReactions();
        Gunther dead = gunther();
        dead.setHp(0);
        Gunther alive = gunther();

        battle(source).resolveAttack(goblin(), List.of(dead, alive), List.of(parryable()));

        assertEquals(List.of(List.of(alive)), source.askedTargets);
        assertEquals(0, dead.getHp());
    }

    @Test void aDeadAttackerDoesNothing()
    {
        ScriptedReactions source = new ScriptedReactions(perfectParry());
        Enemy dead = goblin();
        dead.setHp(0);
        Gunther g = gunther();
        BattleContext battle = battle(source);

        int total = battle.resolveAttack(dead, List.of(g), List.of(parryable()));

        assertEquals(0, total);
        assertEquals(50, g.getHp());
        assertTrue(source.asked.isEmpty());
        assertTrue(battle.drainEvents().isEmpty());              // not even a counter
    }

    @Test void anAttackWithNoLivingTargetDoesNothing()
    {
        Gunther dead = gunther();
        dead.setHp(0);
        BattleContext battle = battle(ReactionSource.none());

        assertEquals(0, battle.resolveAttack(goblin(), List.of(dead), List.of(parryable())));
        assertTrue(battle.drainEvents().isEmpty());
    }

    // ---------- resolveAttack: events and the clock ----------

    @Test void theEventsOfAnAttackTellWhatEveryHitDid()
    {
        Gunther g = gunther();
        Enemy attacker = goblin();
        BattleContext battle = battle(new ScriptedReactions(perfectParry(), ReactionInput.none()));

        battle.resolveAttack(attacker, List.of(g), List.of(parryable(), hit(300, ReactionOptions.PARRY_ONLY, StrikeWeight.NORMAL)));

        ReactionOutcome parry = new ReactionOutcome(ReactionType.PARRY, ReactionQuality.PERFECT);
        assertEquals(List.of(
                        new BattleEvent.HitResolved(attacker, g, parry, 0, false, 500),
                        new BattleEvent.HitResolved(attacker, g, ReactionOutcome.noReaction(), 17, false, 800)),
                battle.drainEvents());
    }

    @Test void anAttackOnSeveralTargetsResolvesSimultaneously()
    {
        BattleContext battle = battle(ReactionSource.none());
        Enemy attacker = goblin();
        Gunther g1 = gunther();
        Gunther g2 = gunther();

        // Testing the new AoE simultaneous group logic
        battle.resolveAttack(attacker, List.of(g1, g2), List.of(parryable()));

        // Both hits trigger simultaneously at exactly the 500ms mark
        assertEquals(List.of(500L, 500L), battle.drainEvents().stream().map(BattleEvent::atMs).toList());
    }

    // ---------- Death ----------

    @Test void theAttackStopsWhenTheTargetDies()
    {
        ScriptedReactions source = new ScriptedReactions();
        Gunther fragile = gunther();
        fragile.setHp(5);                                // the first hit (17) kills
        BattleContext battle = battle(source);

        int total = battle.resolveAttack(goblin(), List.of(fragile), List.of(parryable(), parryable(), parryable()));

        assertEquals(17, total);
        assertFalse(fragile.isAlive());
        assertEquals(1, source.asked.size());            // nobody was asked about the hits that never happened

        List<BattleEvent> events = battle.drainEvents();
        assertEquals(new BattleEvent.Defeated(fragile, 500), events.getLast());
        assertEquals(2, events.size());                  // the hit and the defeat
    }

    @Test void aDeadTargetGivesNoCounter()
    {
        Enemy attacker = goblin();
        Gunther fragile = gunther();
        fragile.setHp(1);

        battle(new ScriptedReactions(earlyParry())).resolveAttack(attacker, List.of(fragile), List.of(parryable()));

        assertEquals(40, attacker.getHp());
    }

    @Test void aCounterCanDefeatTheAttacker()
    {
        Enemy weak = goblin(5);
        Gunther g = gunther();
        BattleContext battle = battle(new ScriptedReactions(perfectParry()));

        battle.resolveAttack(weak, List.of(g), List.of(parryable()));

        assertFalse(weak.isAlive());
        assertEquals(new BattleEvent.Defeated(weak, 500), battle.drainEvents().getLast());
    }

    // ---------- heal ----------

    @Test void healRestoresHealthAndTellsIt()
    {
        BattleContext battle = battle(ReactionSource.none());
        Enemy shaman = goblin();
        shaman.setHp(10);

        int restored = battle.heal(shaman, shaman, 15);

        assertEquals(15, restored);
        assertEquals(25, shaman.getHp());
        assertEquals(List.of(new BattleEvent.Healed(shaman, shaman, 15, 0)), battle.drainEvents());
    }

    @Test void healNeverGoesAboveTheMaxHealthAndOnlyCountsWhatWasRestored()
    {
        BattleContext battle = battle(ReactionSource.none());
        Enemy shaman = goblin();
        shaman.setHp(35);

        assertEquals(5, battle.heal(shaman, shaman, 15));
        assertEquals(40, shaman.getHp());
    }

    @Test void healNeverRevives()
    {
        BattleContext battle = battle(ReactionSource.none());
        Enemy dead = goblin();
        dead.setHp(0);

        assertEquals(0, battle.heal(goblin(), dead, 20));
        assertFalse(dead.isAlive());
        assertTrue(battle.drainEvents().isEmpty());
    }

    @Test void healingSomeoneAtFullHealthTellsNothing()
    {
        BattleContext battle = battle(ReactionSource.none());
        Enemy shaman = goblin();

        assertEquals(0, battle.heal(shaman, shaman, 10));
        assertTrue(battle.drainEvents().isEmpty());
    }

    // ---------- Validation ----------

    @Test void invalidArgumentsThrow()
    {
        BattleContext battle = battle(ReactionSource.none());
        Gunther g = gunther();
        Enemy e = goblin();

        assertThrows(IllegalArgumentException.class, () -> battle.resolveAttack(e, List.of(g), List.of()));
        assertThrows(IllegalArgumentException.class, () -> battle.heal(e, e, -1));
        assertThrows(NullPointerException.class, () -> battle.resolveHit(null, e, parryable()));
        assertThrows(NullPointerException.class, () -> battle.resolveHit(g, e, null));
        assertThrows(NullPointerException.class, () -> battle.resolveAttack(e, null, List.of(parryable())));
        assertThrows(NullPointerException.class, () -> battle.resolveAttack(e, List.of(g), null));
        assertThrows(NullPointerException.class, () -> battle.heal(null, e, 5));
        assertThrows(IllegalArgumentException.class, () -> battle.resolveAttack(e, List.of(), List.of(parryable())));
    }

    @Test void aSourceThatReturnsNothingIsAnError()
    {
        BattleContext battle = battle((target, hit) -> null);

        assertThrows(NullPointerException.class, () -> battle.resolveAttack(goblin(), List.of(gunther()), List.of(parryable())));
    }

    @Test void theEngineNeedsAllItsParts()
    {
        ReactionResolver resolver = new ReactionResolver(ReactionWindows.defaults());

        assertThrows(NullPointerException.class, () -> new BattleContext(null, resolver, ReactionSource.none(), BattleRules.defaults()));
        assertThrows(NullPointerException.class, () -> new BattleContext(new DamageCalculator(), null, ReactionSource.none(), BattleRules.defaults()));
        assertThrows(NullPointerException.class, () -> new BattleContext(new DamageCalculator(), resolver, null, BattleRules.defaults()));
        assertThrows(NullPointerException.class, () -> new BattleContext(new DamageCalculator(), resolver, ReactionSource.none(), null));
        assertThrows(NullPointerException.class, () -> new BattleContext((ReactionSource) null));
    }

    @Test void theDefaultEngineWorks()
    {
        Enemy target = goblin();

        assertTrue(new BattleContext(ReactionSource.none()).resolveHit(gunther(), target, parryable()).amount() >= 1);
    }

    // ---------- The same hit on several targets ----------

    @Test void aHitOnSeveralTargetsLandsOnAllOfThemAtTheSameMoment()
    {
        BattleContext battle = battle(ReactionSource.none());
        Gunther g = gunther();
        Enemy first = goblin();
        Enemy second = goblin();

        List<DamageResult> results = battle.resolveHit(g, List.of(first, second), hit(400, ReactionOptions.NONE, StrikeWeight.NORMAL));

        assertEquals(2, results.size());
        assertEquals(40 - 18, first.getHp());
        assertEquals(40 - 18, second.getHp());
        assertEquals(List.of(400L, 400L), battle.drainEvents().stream().map(BattleEvent::atMs).toList());
    }

    @Test void aGroupHitMovesTheClockOnlyOnce()
    {
        BattleContext battle = battle(ReactionSource.none());
        Gunther g = gunther();

        battle.resolveHit(g, List.of(goblin(), goblin(), goblin()), hit(400, ReactionOptions.NONE, StrikeWeight.NORMAL));
        battle.drainEvents();
        battle.resolveHit(g, goblin(), hit(300, ReactionOptions.NONE, StrikeWeight.NORMAL));

        assertEquals(700, battle.drainEvents().getFirst().atMs());       // not 400 * 3 + 300
    }

    @Test void aGroupHitSkipsTheFallen()
    {
        BattleContext battle = battle(ReactionSource.none());
        Enemy fallen = goblin();
        fallen.setHp(0);
        Enemy alive = goblin();

        List<DamageResult> results = battle.resolveHit(gunther(), List.of(fallen, alive), parryable());

        assertEquals(1, results.size());
        assertEquals(0, fallen.getHp());
        assertEquals(40 - 18, alive.getHp());
    }

    @Test void aGroupHitOnNobodyDoesNotMoveTheClock()
    {
        BattleContext battle = battle(ReactionSource.none());
        Gunther g = gunther();
        Enemy fallen = goblin();
        fallen.setHp(0);

        assertTrue(battle.resolveHit(g, List.of(fallen), hit(400, ReactionOptions.NONE, StrikeWeight.NORMAL)).isEmpty());
        battle.resolveHit(g, goblin(), hit(300, ReactionOptions.NONE, StrikeWeight.NORMAL));

        assertEquals(300, battle.drainEvents().getLast().atMs());
    }

    @Test void aGroupHitNeedsTargets()
    {
        BattleContext battle = battle(ReactionSource.none());

        assertThrows(IllegalArgumentException.class, () -> battle.resolveHit(gunther(), List.<Fighter>of(), parryable()));
        assertThrows(NullPointerException.class, () -> battle.resolveHit(gunther(), (List<Fighter>) null, parryable()));
    }
}