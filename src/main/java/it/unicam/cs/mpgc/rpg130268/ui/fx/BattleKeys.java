package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenter;
import javafx.scene.input.KeyCode;

import java.util.Objects;
import java.util.function.Supplier;

/* *
    What every key does in a battle. It depends on what the player is doing: choosing in the menu (F, E, W...),
    choosing the target (A, D, Enter), reacting to an attack (E, Q), answering the question at the end. They never happen
    together, so E can be both. T shows/hides the timing bar and L the log
* */
final class BattleKeys
{
    private final Supplier<BattlePresenter> current;
    private final BattleMenu menu;
    private final TargetSelection targeting;
    private final BattleEnding ending;
    private final Runnable toggleTimingBar;
    private final Runnable toggleLog;
    private final Runnable catchUp;

    BattleKeys(Supplier<BattlePresenter> current, BattleMenu menu, TargetSelection targeting, BattleEnding ending,
               Runnable toggleTimingBar, Runnable toggleLog, Runnable catchUp)
    {
        this.current = Objects.requireNonNull(current, "current must not be null");
        this.menu = Objects.requireNonNull(menu, "menu must not be null");
        this.targeting = Objects.requireNonNull(targeting, "targeting must not be null");
        this.ending = Objects.requireNonNull(ending, "ending must not be null");
        this.toggleTimingBar = Objects.requireNonNull(toggleTimingBar, "toggleTimingBar must not be null");
        this.toggleLog = Objects.requireNonNull(toggleLog, "toggleLog must not be null");
        this.catchUp = Objects.requireNonNull(catchUp, "catchUp must not be null");
    }

    void onKey(KeyCode code)
    {
        BattlePresenter presenter = current.get();

        if (presenter == null) return;

        if (code == KeyCode.T)
            toggleTimingBar.run();
        else if (code == KeyCode.L)
            toggleLog.run();
        else if (presenter.isOver())
            ending.onKey(code);
        else if (presenter.isTargeting())
            targetKey(presenter, code);
        else if (presenter.isChoosing())
            menu.onKey(code);
        else if (code == KeyCode.E)
            react(presenter, ReactionType.PARRY);
        else if (code == KeyCode.Q)
            react(presenter, ReactionType.DODGE);
    }

    private void targetKey(BattlePresenter presenter, KeyCode code)
    {
        if (code == KeyCode.A || code == KeyCode.LEFT)
            targeting.move(-1);
        else if (code == KeyCode.D || code == KeyCode.RIGHT)
            targeting.move(1);
        else if (code == KeyCode.ENTER)
            targeting.confirm();
        else if (code == KeyCode.ESCAPE)
            presenter.cancel();
    }

    private void react(BattlePresenter presenter, ReactionType type)
    {
        catchUp.run();
        presenter.press(type);
    }

    // The keys that work now, written in the bottom-left corner
    String hint()
    {
        BattlePresenter presenter = current.get();

        if (presenter.isOver())
            return "";

        if (presenter.isTargeting())
            return "A / D -- change target        ENTER -- confirm        ESC -- go back";

        if (presenter.isChoosing())
            return menu.isOnAbilitiesPage() ? "ESC -- go back" : "";

        return "E -- parry        Q -- dodge";
    }
}
