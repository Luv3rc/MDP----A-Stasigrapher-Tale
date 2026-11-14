package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

final class ChoiceMenu
{
    private final Battle battle;
    private final BiConsumer<Ability, List<Fighter>> performer;

    private boolean alwaysAskTarget = false;        // ask for the target even if only one can be chosen
    private Ability chosenAbility = null;
    private List<Fighter> candidates = List.of();

    ChoiceMenu(Battle battle, BiConsumer<Ability, List<Fighter>> performer)
    {
        this.battle = Objects.requireNonNull(battle, "battle must not be null");
        this.performer = Objects.requireNonNull(performer, "performer must not be null");
    }

    void setAlwaysAskTarget(boolean alwaysAskTarget) { this.alwaysAskTarget = alwaysAskTarget; }

    void chooseAbility(Ability ability)
    {
        Fighter actor = battle.getCurrentActor().orElseThrow();

        switch (ability.getTargetType())
        {
            case SELF -> perform(ability, List.of(actor));
            case ALL_ENEMIES -> perform(ability, battle.enemiesOf(actor));
            case ALL_ALLIES -> perform(ability, battle.alliesOf(actor));
            case ONE_ENEMY -> pickOne(ability, battle.enemiesOf(actor));
            case ONE_ALLY -> pickOne(ability, battle.alliesOf(actor));
        }
    }

    void chooseTarget(Fighter target)
    {
        if (chosenAbility == null || !candidates.contains(target)) return;

        perform(chosenAbility, List.of(target));
    }

    void cancel()
    {
        chosenAbility = null;
        candidates = List.of();
    }

    boolean isTargeting() { return chosenAbility != null; }

    List<Fighter> candidates() { return candidates; }

    private void pickOne(Ability ability, List<Fighter> pool)
    {
        if (pool.size() == 1 && !alwaysAskTarget)
        {
            perform(ability, pool);
            return;
        }

        chosenAbility = ability;
        candidates = List.copyOf(pool);
    }

    private void perform(Ability ability, List<Fighter> targets)
    {
        cancel();
        performer.accept(ability, targets);
    }
}
