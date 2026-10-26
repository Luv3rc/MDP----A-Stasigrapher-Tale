package it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.FirstUsableAbility;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAttack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PerEnemyBehaviorTest
{
    private static List<Hit> swing() { return List.of(new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL)); }

    // Both know the attacks "First" and "Second"
    private Enemy enemy(String name)
    {
        Enemy enemy = new Enemy(name, new Stats(40, 5, 2, 3, 0));
        enemy.learn(e -> new EnemyAttack(e, "First", TargetType.ONE_ENEMY, swing()));
        enemy.learn(e -> new EnemyAttack(e, "Second", TargetType.ONE_ENEMY, swing()));
        return enemy;
    }

    private final Fighter foe = new Enemy("Foe", new Stats(40, 5, 2, 3, 0));

    private String choose(EnemyBehavior behavior, Enemy self)
    {
        return behavior.choose(self, List.of(self), List.of(foe)).orElseThrow().ability().getName();
    }

    @Test void everyEnemyFollowsItsOwnBehavior()
    {
        Enemy goblin = enemy("Goblin");
        Enemy brute = enemy("Brute");
        PerEnemyBehavior behavior = new PerEnemyBehavior(new FirstUsableAbility())
                .assign(goblin, new RotationBehavior(List.of("Second")))
                .assign(brute, new RotationBehavior(List.of("First")));

        assertEquals("Second", choose(behavior, goblin));
        assertEquals("First", choose(behavior, brute));
    }

    @Test void theOnesWithoutOneUseTheFallback()
    {
        Enemy stranger = enemy("Stranger");
        PerEnemyBehavior behavior = new PerEnemyBehavior(new FirstUsableAbility())
                .assign(enemy("Goblin"), new RotationBehavior(List.of("Second")));

        assertEquals("First", choose(behavior, stranger));   // the first ability it knows
    }

    @Test void assigningAgainReplacesTheBehavior()
    {
        Enemy goblin = enemy("Goblin");
        PerEnemyBehavior behavior = new PerEnemyBehavior(new FirstUsableAbility())
                .assign(goblin, new RotationBehavior(List.of("First")))
                .assign(goblin, new RotationBehavior(List.of("Second")));

        assertEquals("Second", choose(behavior, goblin));
    }

    @Test void itNeedsItsParts()
    {
        PerEnemyBehavior behavior = new PerEnemyBehavior(new FirstUsableAbility());

        assertThrows(NullPointerException.class, () -> new PerEnemyBehavior(null));
        assertThrows(NullPointerException.class, () -> behavior.assign(null, new FirstUsableAbility()));
        assertThrows(NullPointerException.class, () -> behavior.assign(enemy("Goblin"), null));
    }
}