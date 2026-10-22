package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageResult;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionInput;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionResolver;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;

import java.util.List;
import java.util.Objects;

/* *
    The REAL TIME engine: the clock, the animations and the keys of the player are all in sync because they all
    use the same BattleClock. Nobody waits for anybody -- the UI only will, every frame,
        battle.update(deltaMs);           // time goes on and what is due gets resolved
        battle.react(ReactionType.PARRY); // when the player presses a key
        battle.drainEvents();             // what happened, to show it

    - An enemy attack (resolveAttack) does not resolve at once, it is scheduled on the clock, the hits land one
      after the other, and AttackStarted will tell the UI when. A hit is judged when its reaction window has closed
      (the impact + how late a reaction can still be good). By then every key the player pressed is known
    - react() is stamped with the time of the clock. The FIRST key pressed that a hit can take counts for that hit,
      and a key pressed before the wind-up of a hit begins is ignored
    - Only one attack is in progress at a time (isBusy()), the next action waits for the end of the last one
    - The player's own strikes (resolveHit) and heals don't need reactions, so they are resolved at once as in
      BattleContext, but their events are stamped on the same clock, after whatever was scheduled before

    The events of a hit are stamped with the moment of its IMPACT, but the engine only knows the result when the
    reaction window closes, so they reach the UI a little later (at most the length of that window)
* */
public class LiveBattleContext extends BattleContext
{
    private final ReactionWindows windows;

    // The attack in progress, if there is one
    private LiveAttack current = null;

    public LiveBattleContext(DamageCalculator calculator, ReactionWindows windows, BattleRules rules, BattleClock clock)
    {
        // Nobody is asked about the reactions: the player tells them by pressing keys (see react)
        super(calculator, new ReactionResolver(windows), ReactionSource.none(), rules, clock);
        this.windows = windows;
    }

    // With all the default rules
    public LiveBattleContext(BattleClock clock)
    {
        this(new DamageCalculator(), ReactionWindows.defaults(), BattleRules.defaults(), clock);
    }

    // True while an attack is being played: nothing else can be resolved until it ends
    public boolean isBusy() { return current != null; }

    // Time goes on by deltaMs: the clock moves and every hit whose reaction window has closed gets resolved
    public void update(long deltaMs)
    {
        clock().advance(deltaMs);
        resolveDue();
    }

    // The player pressed a key NOW (on the battle clock). Returns false when it counts for nothing: there is no
    // attack, no hit that accepts it, or it is too early or too late for all of them
    public boolean react(ReactionType type)
    {
        Objects.requireNonNull(type, "type must not be null");

        if (type == ReactionType.NONE)
            throw new IllegalArgumentException("not pressing anything is not a reaction");

        return current != null && current.take(type, clock().getNowMs());
    }

    @Override
    public DamageResult resolveHit(Fighter attacker, Fighter target, Hit hit, double bonusMultiplier)
    {
        requireIdle();
        return super.resolveHit(attacker, target, hit, bonusMultiplier);
    }

    @Override
    public int heal(Fighter source, Fighter target, int amount)
    {
        requireIdle();
        return super.heal(source, target, amount);
    }

    // It only schedules the attack: the damage is not known until its hits land. So it returns 0, and
    // the events tell what happened (hit by hit)
    @Override
    public int resolveAttack(Fighter attacker, List<Fighter> targets, List<Hit> hits)
    {
        requireIdle();

        AttackInProgress attack = new AttackInProgress(attacker, targets, hits);

        // A dead attacker, or nobody left to hit: there is nothing to play
        if (attack.isOver()) return 0;

        long start = startAction();
        current = new LiveAttack(attack, List.copyOf(hits), start);
        reserveUntil(current.lastImpact());

        record(new BattleEvent.AttackStarted(attacker, List.copyOf(targets), List.copyOf(hits), start));

        // A hit that lands at once, and can't be reacted to, is already due
        resolveDue();

        return 0;
    }

    private void requireIdle()
    {
        if (current != null)
            throw new IllegalStateException("an attack is still in progress");
    }

    private void resolveDue()
    {
        while (current != null)
        {
            if (current.isOver())
            {
                current.finish();
                current = null;
            }
            else if (current.nextDeadline() <= clock().getNowMs())
                current.resolveNext();
            else
                break;
        }
    }

    // How late a reaction can still be good for a hit with these options (0 if it can't be reacted to)
    private long lateWindowMs(ReactionOptions options)
    {
        return switch (options)
        {
            case NONE -> 0;
            case PARRY_ONLY -> lateWindowMs(ReactionType.PARRY);
            case DODGE_ONLY -> lateWindowMs(ReactionType.DODGE);
            case PARRY_OR_DODGE -> Math.max(lateWindowMs(ReactionType.PARRY), lateWindowMs(ReactionType.DODGE));
        };
    }

    private long lateWindowMs(ReactionType type)
    {
        return type == ReactionType.PARRY ? windows.lateParryWindowMs() : windows.lateDodgeWindowMs();
    }

    /* *
        An attack on the clock: when every hit starts and lands, and what the player pressed for each one.
        The hit i starts when the hit i-1 lands (the first one when the attack starts) and lands windUpMs later
    */
    private class LiveAttack
    {
        private final AttackInProgress attack;
        private final List<Hit> hits;
        private final long[] startMs;
        private final long[] impactMs;
        private final ReactionInput[] inputs;
        private int resolved = 0;

        LiveAttack(AttackInProgress attack, List<Hit> hits, long start)
        {
            this.attack = attack;
            this.hits = hits;
            this.startMs = new long[hits.size()];
            this.impactMs = new long[hits.size()];
            this.inputs = new ReactionInput[hits.size()];

            long time = start;
            for (int i = 0; i < hits.size(); i++)
            {
                startMs[i] = time;
                time += hits.get(i).windUpMs();
                impactMs[i] = time;
            }
        }

        long lastImpact() { return impactMs[impactMs.length - 1]; }

        boolean isOver() { return attack.isOver(); }

        // When the next hit can be judged: its reaction window has closed
        long nextDeadline() { return impactMs[resolved] + lateWindowMs(hits.get(resolved).options()); }

        // Gives the key to the first hit that can take it
        boolean take(ReactionType type, long nowMs)
        {
            for (int i = resolved; i < hits.size(); i++)
            {
                // Nothing to react to yet: this hit has not started
                if (nowMs < startMs[i]) return false;

                Hit hit = hits.get(i);

                // Already has its key, or does not accept this one
                if (inputs[i] != null || !hit.options().allows(type)) continue;

                // Too late even for this hit's window: it can only be meant for the next hit
                if (nowMs > impactMs[i] + lateWindowMs(type)) continue;

                inputs[i] = new ReactionInput(type, nowMs - startMs[i]);
                return true;
            }

            return false;
        }

        void resolveNext()
        {
            int i = resolved++;
            ReactionInput input = inputs[i] != null ? inputs[i] : ReactionInput.none();

            attack.resolveNext(judge(input, hits.get(i)), impactMs[i]);
        }

        void finish() { attack.finish(resolved == 0 ? startMs[0] : impactMs[resolved - 1]); }
    }
}