package it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.EnemyAction;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

// The abilities (by name) one after the other, turn after turn, and then again from the start
// If the one whose turn it is can't be used (e.g. a heal at full health), the next one is tried
// It remembers where it is, so every enemy needs its own (see PerEnemyBehavior)
public class RotationBehavior implements EnemyBehavior
{
    private final List<String> order;
    private int next = 0;

    public RotationBehavior(List<String> abilityNames)
    {
        Objects.requireNonNull(abilityNames, "abilityNames must not be null");

        if (abilityNames.isEmpty())
            throw new IllegalArgumentException("a rotation needs at least one ability");

        this.order = List.copyOf(abilityNames);     // it also refuses null names
    }

    @Override
    public Optional<EnemyAction> choose(Enemy self, List<Fighter> allies, List<Fighter> foes)
    {
        for (int tried = 0; tried < order.size(); tried++)
        {
            String name = order.get(next);
            next = (next + 1) % order.size();

            Optional<EnemyAction> action = EnemyTargeting.abilityNamed(self, name)
                    .flatMap(ability -> EnemyTargeting.actionFor(self, ability, allies, foes));

            if (action.isPresent()) return action;
        }

        return Optional.empty();
    }
}