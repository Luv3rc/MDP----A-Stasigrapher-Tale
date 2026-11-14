package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleOutcome;
import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenter;

import java.util.Objects;

final class EndOfBattle
{
    private static final long PAUSE_NANOS = 1_200_000_000L;

    private final BattleEnding ending;
    private long seenNanos;
    private boolean done;

    EndOfBattle(BattleEnding ending)
    {
        this.ending = Objects.requireNonNull(ending, "ending must not be null");
    }

    // A new battle begins
    void reset()
    {
        seenNanos = 0;
        done = false;
    }

    void check(BattlePresenter presenter, long nowNanos)
    {
        if (done || !presenter.isOver())
            return;

        if (seenNanos == 0)
        {
            seenNanos = nowNanos;
            return;
        }

        if (nowNanos - seenNanos < PAUSE_NANOS)
            return;

        done = true;
        BattleEvent.BattleEnded end = presenter.getEnd().orElseThrow();

        if (end.outcome() == BattleOutcome.VICTORY)
            ending.onWon(end);
        else
            ending.onLost();
    }
}
