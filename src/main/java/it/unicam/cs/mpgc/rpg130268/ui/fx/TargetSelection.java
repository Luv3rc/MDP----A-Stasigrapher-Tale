package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/* *
    Which of the fighters that can be chosen is selected now. The target is chosen on the battlefield, not in a menu:
    A / D move the selection from one sprite to the next, Enter confirms.
    "Next" is decided by the position on the screen (from the left to the right), so A and D go where they say,
    wherever the enemies are placed.
* */
final class TargetSelection
{
    private final Map<Fighter, FighterCard> cards;           // to know where every sprite is
    private final Supplier<BattlePresenter> current;

    private int index = 0;              // which of the candidates (from the left) is selected

    TargetSelection(Map<Fighter, FighterCard> cards, Supplier<BattlePresenter> current)
    {
        this.cards = Objects.requireNonNull(cards, "cards must not be null");
        this.current = Objects.requireNonNull(current, "current must not be null");
    }

    // Nobody is being chosen: the next time it starts from the first one
    void reset() { index = 0; }

    // The selection goes to the next sprite on the left (-1) or on the right (+1), and round again
    void move(int step)
    {
        int count = candidates().size();
        if (count > 0) index = Math.floorMod(index + step, count);
    }

    // Enter: the selected sprite is the target
    void confirm()
    {
        Fighter selected = selected(candidates());
        if (selected != null) current.get().chooseTarget(selected);
    }

    // The fighters that can be chosen, from the left of the screen to the right (so A and D go where they say)
    List<Fighter> candidates()
    {
        return current.get().getTargetCandidates().stream()
                .sorted(Comparator.comparingDouble(this::screenX))
                .toList();
    }

    // The selected one among these candidates (null if there is none)
    Fighter selected(List<Fighter> candidates)
    {
        return candidates.isEmpty() ? null : candidates.get(Math.floorMod(index, candidates.size()));
    }

    private double screenX(Fighter fighter)
    {
        FighterCard card = cards.get(fighter);
        return card == null ? 0 : card.getRoot().localToScene(card.getRoot().getBoundsInLocal()).getMinX();
    }
}