package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageResult;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageSpec;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionInput;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOutcome;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionResolver;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;
import it.unicam.cs.mpgc.rpg130268.combat.skills.CombatContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* *
 * The engine behind the abilities. Abilities say WHAT happens, this class decides HOW:
 * - it calculates and applies the damage (DamageCalculator)
 * - it asks for the reaction of the targets to every hit that can be reacted to (ReactionSource),
 *   and judges it (ReactionResolver)
 * - a perfect parry gives the defender what its onPerfectParry() gives; parrying EVERY hit of an attack
 *   gives a counter on the attacker
 * - it NEVER waits. It resolves everything at once and writes down WHEN every thing happens, on the battle
 *   clock (see BattleClock): every hit takes its wind-up from where the last thing ended, and never in the past.
 *   The UI takes the events with drainEvents() and replays them over time
 *
 * A hit eats EVERYONE it is aimed at (Expedition style): there is ONE reaction per hit, and it counts for all
 * the targets. If it fails everyone takes the damage; if it is a perfect parry everyone parries (and gets what a
 * parry gives). Everyone who parried every hit counters. All the targets of a hit are hit at the same moment
 *
 * This is the INSTANT engine: it asks the ReactionSource and gets the answer at once (good for the tests and
 * for fights nobody watches). For a real time fight, where the player presses a key WHILE the attack is
 * happening, see LiveBattleContext
 * */
public class BattleContext implements CombatContext
{
    private final DamageCalculator calculator;
    private final ReactionResolver resolver;
    private final ReactionSource reactions;
    private final BattleRules rules;
    private final BattleClock clock;

    private final List<BattleEvent> events = new ArrayList<>();

    // Where the last thing that was scheduled ends, on the battle clock. The next one starts there (or now)
    private long cursorMs = 0;

    public BattleContext(DamageCalculator calculator, ReactionResolver resolver, ReactionSource reactions,
                         BattleRules rules, BattleClock clock)
    {
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.reactions = Objects.requireNonNull(reactions, "reactions must not be null");
        this.rules = Objects.requireNonNull(rules, "rules must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // With a clock of its own (nobody moves it: the events are timed from 0)
    public BattleContext(DamageCalculator calculator, ReactionResolver resolver, ReactionSource reactions, BattleRules rules)
    {
        this(calculator, resolver, reactions, rules, new BattleClock());
    }

    // With all the default rules
    public BattleContext(ReactionSource reactions)
    {
        this(new DamageCalculator(), new ReactionResolver(ReactionWindows.defaults()), reactions, BattleRules.defaults());
    }

    // The events since the last call, in order
    public List<BattleEvent> drainEvents()
    {
        List<BattleEvent> drained = List.copyOf(events);
        events.clear();
        return drained;
    }

    @Override
    public List<DamageResult> resolveHit(Fighter attacker, List<Fighter> targets, Hit hit, double bonusMultiplier)
    {
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(targets, "targets must not be null");
        Objects.requireNonNull(hit, "hit must not be null");

        if (targets.isEmpty())
            throw new IllegalArgumentException("targets must not be empty");

        List<Fighter> living = targets.stream().filter(Fighter::isAlive).toList();
        if (living.isEmpty()) return List.of();

        // ONE step of the clock for everyone: they are all hit at the same moment
        startAction();
        cursorMs += hit.windUpMs();

        List<DamageResult> results = new ArrayList<>();
        for (Fighter target : living)
        {
            DamageResult result = calculator.calculate(new DamageSpec(hit.strikeWeight(), bonusMultiplier), attacker, target);
            dealDamage(attacker, target, ReactionOutcome.noReaction(), result, cursorMs);
            results.add(result);
        }

        return results;
    }

    @Override
    public int resolveAttack(Fighter attacker, List<Fighter> targets, List<Hit> hits)
    {
        AttackInProgress attack = new AttackInProgress(attacker, targets, hits);
        startAction();

        while (!attack.isOver())
        {
            Hit hit = attack.nextHit();
            cursorMs += hit.windUpMs();

            attack.resolveNext(reactionTo(attack.livingTargets(), hit), cursorMs);
        }

        attack.finish(cursorMs);
        return attack.totalDamage();
    }

    @Override
    public int heal(Fighter source, Fighter target, int amount)
    {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(target, "target must not be null");

        if (amount < 0)
            throw new IllegalArgumentException("amount must not be negative");

        // Healing never revives
        if (!target.isAlive()) return 0;

        int before = target.getHp();
        target.heal(amount);
        int restored = target.getHp() - before;

        // ? Maybe doing something to avoid healing when fully hp--avoiding wasting resources
        if (restored > 0)
            events.add(new BattleEvent.Healed(source, target, restored, startAction()));

        return restored;
    }

    // ---------- What the live engine (LiveBattleContext) shares with this one ----------

    BattleClock clock() { return clock; }

    // Until when something is scheduled (and still to be played) on the battle clock
    long scheduledUntilMs() { return cursorMs; }

    void record(BattleEvent event) { events.add(event); }

    // An action never starts in the past: it starts where the last one ended, or now if that is later
    long startAction()
    {
        cursorMs = Math.max(cursorMs, clock.getNowMs());
        return cursorMs;
    }

    // Something is going to happen until atMs: the next action starts after it
    void reserveUntil(long atMs) { cursorMs = Math.max(cursorMs, atMs); }

    // A hit that can't be reacted to (e.g. the player's own strikes) asks nothing to nobody
    ReactionOutcome judge(ReactionInput input, Hit hit)
    {
        if (hit.options() == ReactionOptions.NONE)
            return ReactionOutcome.noReaction();

        return resolver.resolve(input, hit);
    }

    private ReactionOutcome reactionTo(List<Fighter> targets, Hit hit)
    {
        if (hit.options() == ReactionOptions.NONE)
            return ReactionOutcome.noReaction();

        ReactionInput input = Objects.requireNonNull(reactions.inputFor(targets, hit), "the reaction source returned null");
        return judge(input, hit);
    }

    private void counter(Fighter defender, Fighter attacker, long atMs)
    {
        DamageSpec spec = new DamageSpec(rules.counterWeight());
        DamageResult result = calculator.calculate(spec, defender, attacker);

        attacker.takeDamage(result.amount());
        events.add(new BattleEvent.Countered(defender, attacker, result.amount(), result.critical(), atMs));

        defender.onBasicAttackLanded();

        if (!attacker.isAlive())
            events.add(new BattleEvent.Defeated(attacker, atMs));
    }

    private void dealDamage(Fighter attacker, Fighter target, ReactionOutcome reaction, DamageResult result, long atMs)
    {
        target.takeDamage(result.amount());
        events.add(new BattleEvent.HitResolved(attacker, target, reaction, result.amount(), result.critical(), atMs));

        if (!target.isAlive())
            events.add(new BattleEvent.Defeated(target, atMs));
    }

    /* *
        An attack that is being resolved one hit at a time. What has happened so far (the damage, and whether the
        targets have parried every hit up to now)
        The instant engine resolves all its hits in a row; the live engine resolves each one when its time comes
    */
    class AttackInProgress
    {
        private final Fighter attacker;
        private final List<Fighter> targets;
        private final List<Hit> hits;

        private int next = 0;
        private int totalDamage = 0;

        // The reaction is the same for every living target, so either all of them parried a hit or none did
        private boolean parriedEveryHit = true;

        AttackInProgress(Fighter attacker, List<Fighter> targets, List<Hit> hits)
        {
            this.attacker = Objects.requireNonNull(attacker, "attacker must not be null");
            this.targets = Objects.requireNonNull(targets, "targets must not be null");
            this.hits = Objects.requireNonNull(hits, "hits must not be null");

            if (targets.isEmpty())
                throw new IllegalArgumentException("targets must not be empty");

            if (hits.isEmpty())
                throw new IllegalArgumentException("an attack needs at least one hit");
        }

        // No more hits to resolve. All of them were, or the attacker died, or there is nobody left to hit
        boolean isOver() { return next >= hits.size() || !attacker.isAlive() || livingTargets().isEmpty(); }

        Hit nextHit() { return hits.get(next); }

        List<Fighter> livingTargets() { return targets.stream().filter(Fighter::isAlive).toList(); }

        Fighter firstLivingTarget() { return livingTargets().getFirst(); }

        int totalDamage() { return totalDamage; }

        // Resolves the next hit, which lands at atMs, with the reaction of the group
        void resolveNext(ReactionOutcome reaction, long atMs)
        {
            Hit hit = hits.get(next++);

            for (Fighter target : livingTargets())
            {
                if (reaction.avoidsDamage())
                {
                    // The hit still happens: the UI needs to show it being parried or dodged
                    events.add(new BattleEvent.HitResolved(attacker, target, reaction, 0, false, atMs));
                }
                else
                {
                    DamageResult result = calculator.calculate(new DamageSpec(hit.strikeWeight()), attacker, target);
                    dealDamage(attacker, target, reaction, result, atMs);
                    totalDamage += result.amount();
                }

                if (reaction.isSuccessfulParry())
                    target.onPerfectParry();
                else
                    parriedEveryHit = false;
            }
        }

        // Everyone who parried every hit strikes back, while the attacker is still standing
        void finish(long atMs)
        {
            if (next == 0 || !parriedEveryHit) return;

            for (Fighter defender : livingTargets())
            {
                if (!attacker.isAlive()) break;

                counter(defender, attacker, atMs);
            }
        }
    }
}