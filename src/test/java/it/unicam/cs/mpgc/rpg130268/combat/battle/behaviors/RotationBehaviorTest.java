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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RotationBehaviorTest
{
    private static List<Hit> swing() { return List.of(new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL)); }

    // An enemy with two attacks (A and B) and a heal on itself (Mend: only usable when wounded)
    private Enemy enemy()
    {
        Enemy enemy = new Enemy("Enemy", new Stats(40, 5, 2, 3, 0));
        enemy.learn(e -> new EnemyAttack(e, "A", TargetType.ONE_ENEMY, swing()));
        enemy.learn(e -> new EnemyAttack(e, "B", TargetType.ONE_ENEMY, swing()));
        enemy.learn(e -> new EnemyHeal(e, "Mend", TargetType.SELF, 0.25));
        return enemy;
    }

    private final Fighter foe = new Enemy("Foe", new Stats(40, 5, 2, 3, 0));

    // The names of what the behavior chooses, call after call ("-" when it passes)
    private List<String> choices(EnemyBehavior behavior, Enemy self, int times)
    {
        List<String> names = new ArrayList<>();

        for (int i = 0; i < times; i++)
        {
            Optional<EnemyAction> action = behavior.choose(self, List.of(self), List.of(foe));
            names.add(action.map(a -> a.ability().getName()).orElse("-"));
        }

        return names;
    }

    @Test void itUsesTheAbilitiesOneAfterTheOtherAndThenStartsAgain()
    {
        RotationBehavior rotation = new RotationBehavior(List.of("a", "b"));

        assertEquals(List.of("A", "B", "A", "B", "A"), choices(rotation, enemy(), 5));
    }

    @Test void anAbilityThatCannotBeUsedIsSkippedForTheNextOne()
    {
        Enemy enemy = enemy();
        RotationBehavior rotation = new RotationBehavior(List.of("mend", "a"));

        // At full health the heal can't be used: it goes on to the next one, twice
        assertEquals(List.of("A", "A"), choices(rotation, enemy, 2));

        enemy.setHp(10);
        assertEquals(List.of("Mend"), choices(rotation, enemy, 1));
    }

    @Test void itPassesWhenNothingCanBeUsed()
    {
        assertEquals(List.of("-"), choices(new RotationBehavior(List.of("mend")), enemy(), 1));
    }

    @Test void anAbilityTheEnemyDoesNotKnowIsSkipped()
    {
        assertEquals(List.of("A"), choices(new RotationBehavior(List.of("ghost", "a")), enemy(), 1));
    }

    @Test void everyRotationRemembersItsOwnPlace()
    {
        RotationBehavior first = new RotationBehavior(List.of("a", "b"));
        RotationBehavior second = new RotationBehavior(List.of("a", "b"));
        Enemy enemy = enemy();

        choices(first, enemy, 1);

        assertEquals(List.of("B"), choices(first, enemy, 1));
        assertEquals(List.of("A"), choices(second, enemy, 1));
    }

    @Test void aRotationPointsToTheIdAndNotToTheName()
    {
        Enemy goblin = new Enemy("Goblin", new Stats(40, 5, 2, 3, 0));
        goblin.learn(e -> new EnemyAttack(e, "club-swing", "Heavy club swing", TargetType.ONE_ENEMY, swing()));

        // The name was rewritten and the pattern still works, because it never knew the name
        assertEquals(List.of("Heavy club swing"), choices(new RotationBehavior(List.of("club-swing")), goblin, 1));
    }

    @Test void aNameWhereAnIdIsExpectedIsRefusedAtOnce()
    {
        assertThrows(IllegalArgumentException.class, () -> new RotationBehavior(List.of("Club swing")));
    }

    @Test void aRotationNeedsAbilities()
    {
        assertThrows(IllegalArgumentException.class, () -> new RotationBehavior(List.of()));
        assertThrows(NullPointerException.class, () -> new RotationBehavior(null));
        assertThrows(NullPointerException.class, () -> new RotationBehavior(Arrays.asList("a", null)));
    }
}