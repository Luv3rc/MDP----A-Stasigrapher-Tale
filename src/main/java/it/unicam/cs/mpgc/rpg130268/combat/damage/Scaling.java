package it.unicam.cs.mpgc.rpg130268.combat.damage;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

// How an attack draws its power from the attacker's stats: each stat has a weight
// Scaling.of(STRENGTH)                         -> 100% of strength
// Scaling.of(STRENGTH, 0.6).and(LUCK, 0.4)     -> 60% of strength + 40% of luck
// ...
// The weights do not have to add up to 1. A weight of 1.5 is possible
public record Scaling(Map<ScalingStat, Double> weights)
{
    public Scaling
    {
        Objects.requireNonNull(weights, "Weights must not be null");

        if(weights.isEmpty())
            throw new IllegalArgumentException("An attack must scale on something");

        // Copy into an EnumMap.
        // Added in the same order, so the result of the sum is always the same
        EnumMap<ScalingStat, Double> copy = new EnumMap<>(ScalingStat.class);

        for (Map.Entry<ScalingStat, Double> entry : weights.entrySet())
        {
            Objects.requireNonNull(entry.getKey(), "Stats must not be null");
            Objects.requireNonNull(entry.getValue(), "Weight must not be null");

            if (entry.getValue() <= 0)
                throw new IllegalArgumentException("Weight must be positive");

            copy.put(entry.getKey(), entry.getValue());
        }

        weights = Collections.unmodifiableMap(copy);
    }

    public static Scaling of(ScalingStat stat) { return of(stat, 1.0); }

    public static Scaling of(ScalingStat stat, double weight)
    {
     Objects.requireNonNull(stat, "Stats must not be null");

     return new Scaling(Map.of(stat, weight));
    }

    // Returns a NEW scaling with one more stat
    public Scaling and(ScalingStat stat, double weight)
    {
        Objects.requireNonNull(stat, "Stat must not be null");

        if (weights.containsKey(stat))
            throw new IllegalArgumentException(stat + " is already part of this scaling");

        EnumMap<ScalingStat, Double> more = new EnumMap<>(weights);
        more.put(stat, weight);

        return new Scaling(more);
    }

    public double valueFor(Stats stats)
    {
        Objects.requireNonNull(stats, "Stats must not be null");

        double total = 0;

        for (Map.Entry<ScalingStat, Double> entry : weights.entrySet())
            total += entry.getKey().valueIn(stats) * entry.getValue();

        return total;
    }
}
