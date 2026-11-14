package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import java.util.Optional;

interface BattleEnding extends BattleExit
{
    default Optional<Region> popup() { return Optional.empty(); }

    default boolean showsOutcomeBanner() { return false; }

    default void onKey(KeyCode code) { }

    // A new battle begins
    default void reset() { }

    static BattleEnding story(BattleExit exit)
    {
        return new BattleEnding()
        {
            @Override
            public void onWon(BattleEvent.BattleEnded end) { exit.onWon(end); }

            @Override
            public void onLost() { exit.onLost(); }
        };
    }
}
