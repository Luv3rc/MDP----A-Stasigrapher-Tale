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

        ScriptedReactions(ReactionInput... script) { this.script = new ArrayList<>(List.of(script)); }

        @Override
        public ReactionInput inputFor(Fighter target, Hit hit)
        {
            asked.add(hit);
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

    @Test void drainingGivesTheEventsOnceAndRestartsTheClock()
    {
        BattleContext battle = battle(ReactionSource.none());
        Gunther g = gunther();

        battle.resolveHit(g, goblin(), hit(400, ReactionOptions.NONE, StrikeWeight.NORMAL));
        assertEquals(1, battle.drainEvents().size());
        assertTrue(battle.drainEvents().isEmpty());

        battle.resolveHit(g, goblin(), hit(300, ReactionOptions.NONE, StrikeWeight.NORMAL));
        assertEquals(300, battle.drainEvents().getFirst().atMs());   // the new action starts at 0 ms
    }

    @Test void theDrainedEventsCannotBeChanged()
    {
        BattleContext battle = battle(ReactionSource.none());
        battle.resolveHit(gunther(), goblin(), parryable());

        assertThrows(UnsupportedOperationException.class, () -> battle.drainEvents().clear());
    }

    // ---------- resolveAttack: reactions ----------

    @Test void anAttackNobodyReactsToDealsFullDamage()
    {
        Gunther g = gunther();
        List<Hit> hits = List.of(parryable(), hit(300, ReactionOptions.PARRY_ONLY, StrikeWeight.HEAVY));

        int total = battle(ReactionSource.none()).resolveAttack(goblin(), g, hits);

        // normal 20 against defense 4: 17.6 -> 17, heavy 30: 26.4 -> 26
        assertEquals(43, total);
        assertEquals(50 - 43, g.getHp());
    }

    @Test void theSourceIsAskedAboutEveryHitWithTheHit()
    {
        ScriptedReactions source = new ScriptedReactions();
        Hit second = hit(300, ReactionOptions.DODGE_ONLY, StrikeWeight.LIGHT);

        battle(source).resolveAttack(goblin(), gunther(), List.of(parryable(), second));

        assertEquals(List.of(parryable(), second), source.asked);
    }

    @Test void aPerfectParryAvoidsTheDamageOfThatHit()
    {
        Gunther g = gunther();

        int total = battle(new ScriptedReactions(perfectParry())).resolveAttack(goblin(), g, List.of(parryable(), parryable()));

        assertEquals(17, total);                         // only the second hit landed
        assertEquals(50 - 17, g.getHp());
    }

    @Test void aPerfectParryGivesWhatThePerfectParryGives()
    {
        Gunther g = gunther();
        int before = g.getAp().getCurrentAp();

        battle(new ScriptedReactions(perfectParry(), ReactionInput.none())).resolveAttack(goblin(), g, List.of(parryable(), parryable()));

        assertEquals(before + ApRules.defaults().apGainOnPerfectParry(), g.getAp().getCurrentAp());
    }

    @Test void parryingEveryHitGivesACounter()
    {
        Gunther g = gunther();
        Enemy attacker = goblin();
        BattleContext battle = battle(new ScriptedReactions(perfectParry(), perfectParry()));

        int total = battle.resolveAttack(attacker, g, List.of(parryable(), parryable()));

        assertEquals(0, total);
        assertEquals(50, g.getHp());
        // The counter is a heavy basic attack: 30 against defense 2: 28.1 -> 28
        assertEquals(40 - 28, attacker.getHp());

        List<BattleEvent> events = battle.drainEvents();
        assertEquals(new BattleEvent.Countered(g, attacker, 28, false, 1000), events.getLast());
    }

    @Test void aCounterGivesWhatABasicAttackGives()
    {
        Gunther g = gunther();
        int ap = g.getAp().getCurrentAp();
        int ink = g.getInk().getCurrentInk();

        battle(new ScriptedReactions(perfectParry(), perfectParry())).resolveAttack(goblin(), g, List.of(parryable(), parryable()));

        // two perfect parries, then the counter (a basic attack that landed): AP and ink
        int apGain = 2 * ApRules.defaults().apGainOnPerfectParry() + ApRules.defaults().apGainOnBasicAttack();
        assertEquals(ap + apGain, g.getAp().getCurrentAp());
        assertEquals(ink + InkRules.defaults().inkGainPerHit(), g.getInk().getCurrentInk());
    }

    @Test void theCounterUsesTheDefendersWeapon()
    {
        Gunther g = gunther();
        g.equip(new Weapon("Heavy sword", WeaponType.SWORD, 20, ScalingGrade.NONE));   // 20 * 2 = 40, heavy: 60
        Enemy attacker = goblin(100);

        battle(new ScriptedReactions(perfectParry())).resolveAttack(attacker, g, List.of(parryable()));

        assertEquals(100 - 56, attacker.getHp());        // 60 against defense 2: 56.25 -> 56
    }

    @Test void theCounterWeightComesFromTheRules()
    {
        BattleContext extreme = battle(new ScriptedReactions(perfectParry()), new BattleRules(StrikeWeight.EXTREME));
        Enemy attacker = goblin();

        extreme.resolveAttack(attacker, gunther(), List.of(parryable()));

        assertEquals(40 - 37, attacker.getHp());         // 20 * 2.0 = 40 against defense 2: 37.5 -> 37
    }

    @Test void parryingOnlySomeHitsGivesNoCounter()
    {
        Enemy attacker = goblin();

        battle(new ScriptedReactions(perfectParry(), ReactionInput.none()))
                .resolveAttack(attacker, gunther(), List.of(parryable(), parryable()));

        assertEquals(40, attacker.getHp());
    }

    @Test void aDodgeAvoidsTheDamageButGivesNoApAndNoCounter()
    {
        Gunther g = gunther();
        Enemy attacker = goblin();
        int ap = g.getAp().getCurrentAp();

        int total = battle(new ScriptedReactions(goodDodge())).resolveAttack(attacker, g, List.of(parryable()));

        assertEquals(0, total);
        assertEquals(50, g.getHp());
        assertEquals(ap, g.getAp().getCurrentAp());
        assertEquals(40, attacker.getHp());
    }

    @Test void aMissedParryDoesNotAvoidTheDamage()
    {
        Gunther g = gunther();

        int total = battle(new ScriptedReactions(earlyParry())).resolveAttack(goblin(), g, List.of(parryable()));

        assertEquals(17, total);
        assertEquals(50 - 17, g.getHp());
    }

    @Test void aHitThatCannotBeReactedToDoesNotAskAnyone()
    {
        ScriptedReactions source = new ScriptedReactions(perfectParry());
        Gunther g = gunther();

        int total = battle(source).resolveAttack(goblin(), g, List.of(hit(500, ReactionOptions.NONE, StrikeWeight.NORMAL)));

        assertTrue(source.asked.isEmpty());
        assertEquals(17, total);
    }

    // ---------- resolveAttack: events and the clock ----------

    @Test void theEventsOfAnAttackTellWhatEveryHitDid()
    {
        Gunther g = gunther();
        Enemy attacker = goblin();
        BattleContext battle = battle(new ScriptedReactions(perfectParry(), ReactionInput.none()));

        battle.resolveAttack(attacker, g, List.of(parryable(), hit(300, ReactionOptions.PARRY_ONLY, StrikeWeight.NORMAL)));

        ReactionOutcome parry = new ReactionOutcome(ReactionType.PARRY, ReactionQuality.PERFECT);
        assertEquals(List.of(
                        new BattleEvent.HitResolved(attacker, g, parry, 0, false, 500),
                        new BattleEvent.HitResolved(attacker, g, ReactionOutcome.noReaction(), 17, false, 800)),
                battle.drainEvents());
    }

    @Test void anAttackOnSeveralTargetsPlaysOutTargetByTarget()
    {
        BattleContext battle = battle(ReactionSource.none());
        Enemy attacker = goblin();

        battle.resolveAttack(attacker, gunther(), List.of(parryable()));
        battle.resolveAttack(attacker, gunther(), List.of(parryable()));

        assertEquals(List.of(500L, 1000L), battle.drainEvents().stream().map(BattleEvent::atMs).toList());
    }

    // ---------- Death ----------

    @Test void theAttackStopsWhenTheTargetDies()
    {
        ScriptedReactions source = new ScriptedReactions();
        Gunther fragile = gunther();
        fragile.setHp(5);                                // the first hit (17) kills
        BattleContext battle = battle(source);

        int total = battle.resolveAttack(goblin(), fragile, List.of(parryable(), parryable(), parryable()));

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

        battle(new ScriptedReactions(earlyParry())).resolveAttack(attacker, fragile, List.of(parryable()));

        assertEquals(40, attacker.getHp());
    }

    @Test void aCounterCanDefeatTheAttacker()
    {
        Enemy weak = goblin(5);
        Gunther g = gunther();
        BattleContext battle = battle(new ScriptedReactions(perfectParry()));

        battle.resolveAttack(weak, g, List.of(parryable()));

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

        assertThrows(IllegalArgumentException.class, () -> battle.resolveAttack(e, g, List.of()));
        assertThrows(IllegalArgumentException.class, () -> battle.heal(e, e, -1));
        assertThrows(NullPointerException.class, () -> battle.resolveHit(null, e, parryable()));
        assertThrows(NullPointerException.class, () -> battle.resolveHit(g, e, null));
        assertThrows(NullPointerException.class, () -> battle.resolveAttack(e, null, List.of(parryable())));
        assertThrows(NullPointerException.class, () -> battle.resolveAttack(e, g, null));
        assertThrows(NullPointerException.class, () -> battle.heal(null, e, 5));
    }

    @Test void aSourceThatReturnsNothingIsAnError()
    {
        BattleContext battle = battle((target, hit) -> null);

        assertThrows(NullPointerException.class, () -> battle.resolveAttack(goblin(), gunther(), List.of(parryable())));
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
}