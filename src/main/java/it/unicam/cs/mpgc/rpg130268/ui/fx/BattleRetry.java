package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.ui.question.Choice;
import it.unicam.cs.mpgc.rpg130268.ui.question.QuestionPrompt;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import java.util.Objects;
import java.util.Optional;

final class BattleRetry implements BattleEnding
{
    private final QuestionPopup popup;
    private final QuestionPrompt prompt = new QuestionPrompt();
    private final Runnable retry;

    BattleRetry(RpgButtons buttons, Runnable retry)
    {
        this.retry = Objects.requireNonNull(retry, "retry must not be null");
        this.popup = new QuestionPopup(buttons, () -> answer(true), () -> answer(false));
    }

    @Override
    public Optional<Region> popup() { return Optional.of(popup.getRoot()); }

    @Override
    public boolean showsOutcomeBanner() { return true; }

    @Override
    public void onWon(BattleEvent.BattleEnded end) { ask("Victory!"); }

    @Override
    public void onLost() { ask("Defeat."); }

    @Override
    public void reset()
    {
        prompt.close();
        popup.hide();
    }

    @Override
    public void onKey(KeyCode code)
    {
        if (!prompt.isAsking()) return;

        switch (code)
        {
            case A, LEFT, D, RIGHT ->
            {
                prompt.toggle();
                show();
            }
            case ENTER, SPACE -> answer(prompt.selected() == Choice.CONFIRM);
            case Y -> answer(true);
            case N -> answer(false);
            default -> { }
        }
    }

    private void ask(String result)
    {
        prompt.ask("Do you want to retry the battle?", result, "Yes", "No");
        show();
    }

    private void show() { popup.show(prompt.view().orElseThrow()); }

    private void answer(boolean yes)
    {
        reset();

        if (yes)
            retry.run();
        else
            Platform.exit();
    }
}
