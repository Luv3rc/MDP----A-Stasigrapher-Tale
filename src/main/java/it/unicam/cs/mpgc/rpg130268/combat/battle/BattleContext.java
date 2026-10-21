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
import java.util.LinkedHashSet;
import java.util.Set;

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
   * - it NEVER waits. It resolves everything at once and writes down WHEN every thing happens (a clock that
   *   moves by the wind-up of every hit).
      ? The UI will take the events with drainEvents() and replays them over time

   * A hit eats EVERYONE it is aimed at (Expedition style): there is ONE reaction per hit, and it counts for all
   * the targets. If it fails everyone takes the damage; if it is a perfect parry everyone parries (and gets what a
   * parry gives). Everyone who parried every hit counters. All the targets of a hit are hit at the same moment
* */
public class BattleContext implements CombatContext
{
    private final DamageCalculator calculator;
    private final ReactionResolver resolver;
    private final ReactionSource reactions;
    private final BattleRules rules;

    private final List<BattleEvent> events = new ArrayList<>();
    private long clockMs = 0;

    public BattleContext(DamageCalculator calculator, ReactionResolver resolver, ReactionSource reactions, BattleRules rules)
    {
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.reactions = Objects.requireNonNull(reactions, "reactions must not be null");
        this.rules = Objects.requireNonNull(rules, "rules must not be null");
    }

    // With all the default rules
    public BattleContext(ReactionSource reactions)
    {
        this(new DamageCalculator(), new ReactionResolver(ReactionWindows.defaults()), reactions, BattleRules.defaults());
    }

    // The events since the last call, in order. It also restarts the clock
    public List<BattleEvent> drainEvents()
    {
        List<BattleEvent> drained = List.copyOf(events);
        events.clear();
        clockMs = 0;
        return drained;
    }

    @Override
    public DamageResult resolveHit(Fighter attacker, Fighter target, Hit hit, double bonusMultiplier)
    {
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(hit, "hit must not be null");

        clockMs += hit.windUpMs();

        DamageResult result = calculator.calculate(new DamageSpec(hit.strikeWeight(), bonusMultiplier), attacker, target);
        dealDamage(attacker, target, ReactionOutcome.noReaction(), result);

        return result;
    }

    @Override
    public int resolveAttack(Fighter attacker, List<Fighter> targets, List<Hit> hits)
    {
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(targets, "targets must not be null");
        Objects.requireNonNull(hits, "hits must not be null");

        if (targets.isEmpty())
            throw new IllegalArgumentException("an attack needs at least one target");

        if (hits.isEmpty())
            throw new IllegalArgumentException("an attack needs at least one hit");

        int totalDamage = 0;
        boolean anyHit = false;

        // The ones who can still counter: only those who parried EVERY hit so far
        Set<Fighter> parriedEveryHit = new LinkedHashSet<>(targets.stream().filter(Fighter::isAlive).toList());

        for (Hit hit : hits)
        {
            // A dead attacker can't keep attacking
            if (!attacker.isAlive()) break;

            // The ones who fell are out of the attack: the others keep being hit
            List<Fighter> eaten = targets.stream().filter(Fighter::isAlive).toList();
            if (eaten.isEmpty()) break;

            anyHit = true;
            clockMs += hit.windUpMs();

            // ONE reaction for the whole group: the hit eats everyone
            ReactionOutcome reaction = reactionTo(eaten, hit);

            for (Fighter target : eaten)
            {
                if (reaction.avoidsDamage())
                {
                    // The hit still happens: the UI needs to show it being parried or dodged
                    events.add(new BattleEvent.HitResolved(attacker, target, reaction, 0, false, clockMs));
                }
                else
                {
                    DamageResult result = calculator.calculate(new DamageSpec(hit.strikeWeight()), attacker, target);
                    dealDamage(attacker, target, reaction, result);
                    totalDamage += result.amount();
                }

                if (reaction.isSuccessfulParry())
                    target.onPerfectParry();
                else
                    parriedEveryHit.remove(target);
            }
        }

        // Everyone who parried every hit strikes back (if the attacker is still standing)
        if (anyHit)
        {
            for (Fighter defender : parriedEveryHit)
            {
                if (!attacker.isAlive()) break;

                counter(defender, attacker);
            }
        }

        return totalDamage;
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
            events.add(new BattleEvent.Healed(source, target, restored, clockMs));

        return restored;
    }

    private ReactionOutcome reactionTo(List<Fighter> targets, Hit hit)
    {
        // A hit that can't be reacted to (e.g. the player's own strikes) asks nothing to nobody
        if (hit.options() == ReactionOptions.NONE)
            return ReactionOutcome.noReaction();

        ReactionInput input = Objects.requireNonNull(reactions.inputFor(targets, hit), "the reaction source returned null");
        return resolver.resolve(input, hit);
    }

    private void counter(Fighter defender, Fighter attacker)
    {
        DamageSpec spec = new DamageSpec(rules.counterWeight());
        DamageResult result = calculator.calculate(spec, defender, attacker);

        attacker.takeDamage(result.amount());
        events.add(new BattleEvent.Countered(defender, attacker, result.amount(), result.critical(), clockMs));

        defender.onBasicAttackLanded();

        if (!attacker.isAlive())
            events.add(new BattleEvent.Defeated(attacker, clockMs));
    }

    private void dealDamage(Fighter attacker, Fighter target, ReactionOutcome reaction, DamageResult result)
    {
        target.takeDamage(result.amount());
        events.add(new BattleEvent.HitResolved(attacker, target, reaction, result.amount(), result.critical(), clockMs));

        if (!target.isAlive())
            events.add(new BattleEvent.Defeated(target, clockMs));
    }
}