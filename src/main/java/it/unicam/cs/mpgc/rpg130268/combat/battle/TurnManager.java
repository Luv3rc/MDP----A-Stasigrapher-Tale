package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/* *
    Decides WHO acts and WHEN. It knows nothing about what the actor does: that is up to the caller
    - A battle goes in rounds. In every round each living fighter acts once, the fastest first (see TurnOrder).
      Fighters with the same agility act in the order they were given -- party before enemies
    - onTurnStart() is called on a fighter when ITS turn begins, not when the round does (e.g. a party member
      gets its AP at the start of its own turn)
    - A fighter that dies before its turn is skipped. A fighter that is dead when a round starts is not in it
    - nextActor() is empty when the battle is over, even if the round was not finished
*/
public class TurnManager
{
    private final List<Fighter> party;
    private final List<Fighter> enemies;
    private final Deque<Fighter> queue = new ArrayDeque<>();
    private int round = 0;

    public TurnManager (List<? extends Fighter> party, List<? extends Fighter> enemies)
    {
        Objects.requireNonNull(party, "party must not be null");
        Objects.requireNonNull(enemies, "enemies must not be null");

        if (party.isEmpty() || enemies.isEmpty())
            throw new IllegalArgumentException("a battle need fighter on both sides");

        this.party = List.copyOf(party);
        this.enemies = List.copyOf(enemies);
    }

    public int getRound() { return round; }

    public BattleOutcome getBattleOutcome()
    {
        if (party.stream().noneMatch(Fighter::isAlive)) return BattleOutcome.DEFEAT;
        if (enemies.stream().noneMatch(Fighter::isAlive)) return BattleOutcome.VICTORY;

        return BattleOutcome.ONGOING;
    }

    public boolean isBattleOver() { return getBattleOutcome() != BattleOutcome.ONGOING; }

    // The fighter whose turn it is now, or empty if the battle is over. It starts the fighter's turn
    public Optional<Fighter> nextActor()
    {
        // While the battle goes on, both sides have someone alive, so a new round always has actors
        // and this loop always ends
        while(!isBattleOver())
        {
            if (queue.isEmpty()) startNewRound();

            Fighter actor = queue.poll();

            // It might have died after the round started
            if (actor.isAlive())
            {
                actor.onTurnStart();
                return Optional.of(actor);
            }
        }

        return Optional.empty();
    }

    public void startNewRound()
    {
        round ++;

        List<Fighter> everyone = new ArrayList<>(party);
        everyone.addAll(enemies);

        queue.addAll(TurnOrder.sortByAgility(everyone));
    }
}
