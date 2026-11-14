package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// The domain resolves a combo at once, but stamps its events in the future. This keeps them and gives each one
// back when its time comes, in order
final class EventTimeline
{
    private final Battle battle;
    private final List<BattleEvent> pending = new ArrayList<>();

    EventTimeline(Battle battle)
    {
        this.battle = Objects.requireNonNull(battle, "battle must not be null");
    }

    // Takes what the battle has produced since the last time
    void collect()
    {
        pending.addAll(battle.drainEvents());
        pending.sort(Comparator.comparingLong(BattleEvent::atMs));
    }

    List<BattleEvent> releaseUpTo(long timeMs)
    {
        List<BattleEvent> due = new ArrayList<>();

        while (!pending.isEmpty() && pending.getFirst().atMs() <= timeMs)
            due.add(pending.removeFirst());

        return due;
    }

    // Everything that happened has been given back
    boolean isEmpty() { return pending.isEmpty(); }
}
