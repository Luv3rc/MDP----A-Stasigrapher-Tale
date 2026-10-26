package it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.EnemyAction;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAttack;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyHeal;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class WeightedBehaviorTest
{
    // A random source that gives the numbers we want, in order (and remembers the last bound it was asked for)
    private static class Rolls implements RandomGenerator
    {
        private final int[] values;
        private int next = 0;
        int lastBound = -1;

        Rolls(int... values) { this.values = values; }

        @Override public long nextLong() { return 0; }

        @Override public int nextInt(int bound)
        {
            lastBound = bound;
            return values[next++ % values.length];
        }
    }

    private static List<Hit> swing() { return List.of(new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL)); }

    private Enemy enemy()
    {
        Enemy enemy = new Enemy("Enemy", new Stats(40, 5, 2, 3, 0));
        enemy.learn(e -> new EnemyAttack(e, "A", TargetType.ONE_ENEMY, swing()));
        enemy.learn(e -> new EnemyAttack(e, "B", TargetType.ONE_ENEMY, swing()));
        enemy.learn(e -> new EnemyHeal(e, "Mend", TargetType.SELF, 0.25));
        return enemy;
    }

    private final Fighter foe = new Enemy("Foe", new Stats(40, 5, 2, 3, 0));

    private String choose(WeightedBehavior behavior, Enemy self)
    {
        Optional<EnemyAction> action = behavior.choose(self, List.of(self), List.of(foe));
        return action.map(a -> a.ability().getName()).orElse("-");
    }

    private Map<String, Integer> weights(Object... pairs)
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put((String) pairs[i], (Integer) pairs[i + 1]);
        return map;
    }

    @Test void theHeavierAbilitiesAreChosenMoreOften()
    {
        // A has weight 3 and B weight 1: the rolls 0, 1, 2 are A and the roll 3 is B
        WeightedBehavior behavior = new WeightedBehavior(weights("A", 3, "B", 1), new Rolls(0, 1, 2, 3));
        Enemy enemy = enemy();

        assertEquals("A", choose(behavior, enemy));
        assertEquals("A", choose(behavior, enemy));
        assertEquals("A", choose(behavior, enemy));
        assertEquals("B", choose(behavior, enemy));
    }

    @Test void onlyWhatCanBeUsedTakesPart()
    {
        Rolls rolls = new Rolls(0);
        // At full health the heal can't be used, so the total is 3 + 1 and the heavy weight of the heal is ignored
        WeightedBehavior behavior = new WeightedBehavior(weights("A", 3, "B", 1, "Mend", 10), rolls);

        assertEquals("A", choose(behavior, enemy()));
        assertEquals(4, rolls.lastBound);
    }

    @Test void aWoundedEnemyMayHealItself()
    {
        Enemy enemy = enemy();
        enemy.setHp(10);
        // The total is 3 + 1 + 10: the roll 4 is already in the heal
        WeightedBehavior behavior = new WeightedBehavior(weights("A", 3, "B", 1, "Mend", 10), new Rolls(4));

        assertEquals("Mend", choose(behavior, enemy));
    }

    @Test void itPassesWhenNothingCanBeUsed()
    {
        WeightedBehavior behavior = new WeightedBehavior(weights("Mend", 5, "Ghost", 5), new Rolls(0));

        assertEquals("-", choose(behavior, enemy()));
    }

    @Test void theDefaultRandomSourceWorks()
    {
        WeightedBehavior behavior = new WeightedBehavior(weights("A", 1));

        assertEquals("A", choose(behavior, enemy()));
    }

    @Test void theWeightsMustMakeSense()
    {
        assertThrows(IllegalArgumentException.class, () -> new WeightedBehavior(Map.of(), new Rolls(0)));
        assertThrows(IllegalArgumentException.class, () -> new WeightedBehavior(weights("A", 0), new Rolls(0)));
        assertThrows(IllegalArgumentException.class, () -> new WeightedBehavior(weights("A", -2), new Rolls(0)));
        assertThrows(NullPointerException.class, () -> new WeightedBehavior(null, new Rolls(0)));
        assertThrows(NullPointerException.class, () -> new WeightedBehavior(weights("A", 1), null));
    }
}