package it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.EnemyAction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

// Every turn it picks among the abilities it can use, and the ones with a
// bigger weight are picked more often (weights 3 and 1: 75% and 25%)
public class WeightedBehavior implements EnemyBehavior
{
    private final Map<String, Integer> weights;
    private final RandomGenerator random;

    public WeightedBehavior(Map<String, Integer> weightsByAbilityName, RandomGenerator random)
    {
        Objects.requireNonNull(weightsByAbilityName, "weights must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");

        if (weightsByAbilityName.isEmpty())
            throw new IllegalArgumentException("it needs at least one ability");

        for (Map.Entry<String, Integer> entry : weightsByAbilityName.entrySet())
            if (entry.getValue() <= 0)
                throw new IllegalArgumentException("the weight of " + entry.getKey() + " must be greater than 0");

        this.weights = new LinkedHashMap<>(weightsByAbilityName);
    }

    public WeightedBehavior(Map<String, Integer> weightsByAbilityName)
    {
        this(weightsByAbilityName, RandomGenerator.getDefault());
    }

    @Override
    public Optional<EnemyAction> choose(Enemy self, List<Fighter> allies, List<Fighter> foes)
    {
        // Only what can be used now takes part, with its weight
        List<EnemyAction> options = new ArrayList<>();
        List<Integer> optionWeights = new ArrayList<>();
        int total = 0;

        for (Map.Entry<String, Integer> entry : weights.entrySet())
        {
            Optional<EnemyAction> action = EnemyTargeting.abilityNamed(self, entry.getKey())
                    .flatMap(ability -> EnemyTargeting.actionFor(self, ability, allies, foes));

            if (action.isEmpty()) continue;

            options.add(action.get());
            optionWeights.add(entry.getValue());
            total += entry.getValue();
        }

        if (options.isEmpty()) return Optional.empty();

        int roll = random.nextInt(total);

        for (int i = 0; i < options.size(); i++)
        {
            roll -= optionWeights.get(i);

            if (roll < 0) return Optional.of(options.get(i));
        }

        throw new IllegalStateException("the roll is always inside the total weight");
    }
}