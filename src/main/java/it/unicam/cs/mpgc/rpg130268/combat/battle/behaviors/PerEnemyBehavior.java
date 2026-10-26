package it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.EnemyAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// A Battle has ONE behavior for all its enemies. This one gives each enemy its own pattern (a bandit with a
// rotation, a boss with phases) and the ones with none use the fallback
// *e.g. -- new PerEnemyBehavior(new FirstUsableAbility()).assign(goblin, rotation).assign(boss, phases)
// A behavior that remembers things (like RotationBehavior) must belong to one enemy only
public class PerEnemyBehavior implements EnemyBehavior
{
    private final Map<Enemy, EnemyBehavior> behaviors = new HashMap<>();
    private final EnemyBehavior fallback;

    public PerEnemyBehavior(EnemyBehavior fallback)
    {
        this.fallback = Objects.requireNonNull(fallback, "fallback must not be null");
    }

    public PerEnemyBehavior assign(Enemy enemy, EnemyBehavior behavior)
    {
        behaviors.put(Objects.requireNonNull(enemy, "enemy must not be null"),
                Objects.requireNonNull(behavior, "behavior must not be null"));
        return this;
    }

    @Override
    public Optional<EnemyAction> choose(Enemy self, List<Fighter> allies, List<Fighter> foes)
    {
        return behaviors.getOrDefault(self, fallback).choose(self, allies, foes);
    }
}