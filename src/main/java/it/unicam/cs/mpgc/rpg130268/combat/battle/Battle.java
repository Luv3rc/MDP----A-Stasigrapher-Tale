package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/* *
    A whole battle, from the first turn to the XP. It puts together the pieces: the TurnManager (who acts),
    the LiveBattleContext (what happens, on the battle clock), the EnemyBehavior (what the enemies do)
    and the player (through the UI). It is a state machine (see BattlePhase) that moves when
    the UI calls it
        battle.start();                                 // once
        every frame:  battle.update(frameMs);           // time goes on; when an action ends, the next turn begins
                      battle.react(ReactionType.PARRY)  // a key of the player, on the same clock
                      battle.drainEvents()              // what happened, to show it
        when getPhase() == WAITING_FOR_PLAYER:  battle.perform(ability, targets)  // the choice of the player

    - A party member waits for the player; an enemy decides with its behavior and acts at once.
      An enemy with nothing to do passes
    - The next turn begins only when the action in progress has been played (its events, on the clock, are over)
    - When one side has fallen the battle is over. If the party won, every party member who is still alive gets
      (XP / members alive) as reward
* */
public class Battle
{
    private final List<PartyMember> party;
    private final List<Enemy> enemies;
    private final TurnManager turns;
    private final LiveBattleContext context;
    private final EnemyBehavior behavior;

    private BattlePhase phase = BattlePhase.NOT_STARTED;
    private Fighter current = null;

    public Battle(List<PartyMember> party, List<Enemy> enemies, LiveBattleContext context, EnemyBehavior behavior)
    {
        Objects.requireNonNull(party, "party must not be null");
        Objects.requireNonNull(enemies, "enemies must not be null");

        this.party = List.copyOf(party);
        this.enemies = List.copyOf(enemies);
        this.turns = new TurnManager(this.party, this.enemies);     // it checks both sides have fighters
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.behavior = Objects.requireNonNull(behavior, "behavior must not be null");
    }

    // With the default rules and the simplest behavior for the enemies
    public Battle(List<PartyMember> party, List<Enemy> enemies, BattleClock clock)
    {
        this(party, enemies, new LiveBattleContext(clock), new FirstUsableAbility());
    }

    // ---------- Moving the battle ----------

    // Begins the battle: everybody gets ready (AP and ink go back to their starting value) and the first turn starts
    public void start()
    {
        if (phase != BattlePhase.NOT_STARTED)
            throw new IllegalStateException("the battle has already started");

        party.forEach(Fighter::onBattleStart);
        enemies.forEach(Fighter::onBattleStart);

        nextTurn();
    }

    // Time goes on by deltaMs. When the action in progress has been played, the next turn begins.
    // The time goes on in pieces: the next turn begins exactly when the action in progress ends, even if the step
    // is long (a slow frame), so a big step gives the same result as many small ones
    public void update(long deltaMs)
    {
        requireStarted();

        if (deltaMs < 0)
            throw new IllegalArgumentException("deltaMs must not be negative");

        long remaining = deltaMs;

        do
        {
            // While something is being played, stop when it is due: that is when it ends, or a hit is judged
            long step = phase == BattlePhase.PLAYING ? Math.clamp(context.nextDueMs() - now(), 0, remaining) : remaining;

            context.update(step);
            remaining -= step;

            while (phase == BattlePhase.PLAYING && context.isIdle())
                nextTurn();
        }
        while (remaining > 0 && phase == BattlePhase.PLAYING);
    }

    // The choice of the player for the party member whose turn it is. If the ability can't be used on those
    // targets it throws and nothing happens (the turn is still waiting)
    public void perform(Ability ability, List<Fighter> targets)
    {
        Objects.requireNonNull(ability, "ability must not be null");
        Objects.requireNonNull(targets, "targets must not be null");

        if (phase != BattlePhase.WAITING_FOR_PLAYER)
            throw new IllegalStateException("it is not the turn of the player");

        PartyMember actor = (PartyMember) current;
        if (!actor.getListOfAbility().contains(ability))
            throw new IllegalArgumentException(actor.getName() + " does not know " + ability.getName());

        ability.perform(targets, context);
        phase = BattlePhase.PLAYING;
    }

    // A key of the player, on the battle clock (see LiveBattleContext.react)
    public boolean react(ReactionType type) { return context.react(type); }

    // What happened since the last call, in order
    public List<BattleEvent> drainEvents() { return context.drainEvents(); }


    // ---------- Looking at the battle ----------

    public BattlePhase getPhase() { return phase; }

    // Whose turn it is (while the turn lasts), empty before the start and after the end
    public Optional<Fighter> getCurrentActor() { return Optional.ofNullable(current); }

    public BattleOutcome getOutcome() { return turns.getBattleOutcome(); }

    public int getRound() { return turns.getRound(); }

    // The living fighters on the side of this fighter (itself included)
    public List<Fighter> alliesOf(Fighter fighter) { return livingOf(isInParty(fighter)); }

    // The living ones on the other side
    public List<Fighter> enemiesOf(Fighter fighter) { return livingOf(!isInParty(fighter)); }


    // ---------- Inside ----------

    private void requireStarted()
    {
        if (phase == BattlePhase.NOT_STARTED)
            throw new IllegalStateException("the battle has not started");
    }

    private long now() { return context.clock().getNowMs(); }

    private boolean isInParty(Fighter fighter)
    {
        Objects.requireNonNull(fighter, "fighter must not be null");

        if (fighter instanceof PartyMember p && party.contains(p)) return true;
        if (fighter instanceof Enemy e && enemies.contains(e)) return false;

        throw new IllegalArgumentException(fighter.getName() + " is not in this battle");
    }

    private List<Fighter> livingOf(boolean partySide)
    {
        List<Fighter> side = new ArrayList<>(partySide ? party : enemies);
        side.removeIf(fighter -> !fighter.isAlive());
        return side;
    }

    // True while an enemy attack is being played: this is when the keys of the player count
    public boolean isAttackInProgress() { return context.isBusy(); }

    // Starts the turn of the next fighter. An enemy that has nothing to do passes, so this goes on
    // until somebody acts, somebody has to be waited for, or the battle is over
    private void nextTurn()
    {
        while (true)
        {
            Optional<Fighter> next = turns.nextActor();

            if (next.isEmpty())
            {
                finish();
                return;
            }

            current = next.get();
            context.record(new BattleEvent.TurnStarted(current, now()));

            if (current instanceof Enemy enemy)
            {
                if (enemyActs(enemy))
                {
                    phase = BattlePhase.PLAYING;
                    return;
                }
            }
            else
            {
                phase = BattlePhase.WAITING_FOR_PLAYER;
                return;
            }
        }
    }

    // False if the enemy chose to do nothing
    private boolean enemyActs(Enemy enemy)
    {
        Optional<EnemyAction> action = behavior.choose(enemy, alliesOf(enemy), enemiesOf(enemy));

        if (action.isEmpty()) return false;

        action.get().ability().perform(action.get().targets(), context);
        return true;
    }

    private void finish()
    {
        phase = BattlePhase.OVER;
        current = null;

        BattleOutcome outcome = turns.getBattleOutcome();
        int xpPerMember = 0;

        if (outcome == BattleOutcome.VICTORY)
        {
            int totalXp = enemies.stream().mapToInt(Enemy::getXpReward).sum();

            long livingCount = party.stream().filter(PartyMember::isAlive).count();

            if (livingCount > 0)
            {
                xpPerMember = totalXp / (int) livingCount;

                for (PartyMember member : party)
                    if (member.isAlive())
                        member.gainXp(xpPerMember);
            }
        }

        context.record(new BattleEvent.BattleEnded(outcome, xpPerMember, now()));
    }
}