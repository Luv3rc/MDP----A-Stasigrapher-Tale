package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAttack;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyHeal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FirstUsableAbilityTest
{
    private final FirstUsableAbility behavior = new FirstUsableAbility();

    private Enemy enemy(String name, int hp) { return new Enemy(name, new Stats(hp, 5, 2, 3, 0)); }

    private static List<Hit> swing() { return List.of(new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL)); }

    @Test void itUsesTheFirstAbilityItCan()
    {
        Enemy shaman = enemy("Shaman", 40);
        shaman.learn(e -> new EnemyHeal(e, "Mend", TargetType.SELF, 0.25));
        shaman.learn(e -> new EnemyAttack(e, "Staff", TargetType.ONE_ENEMY, swing()));
        Enemy foe = enemy("Foe", 40);

        // Not wounded: the heal can't be used, so it attacks
        assertEquals("Staff", behavior.choose(shaman, List.of(shaman), List.of(foe)).orElseThrow().ability().getName());

        // Wounded: the heal comes first
        shaman.setHp(10);
        assertEquals("Mend", behavior.choose(shaman, List.of(shaman), List.of(foe)).orElseThrow().ability().getName());
    }

    @Test void itAttacksTheFirstFoeWhenTheAttackIsOnOne()
    {
        Enemy goblin = enemy("Goblin", 40);
        goblin.learn(e -> new EnemyAttack(e, "Club", TargetType.ONE_ENEMY, swing()));
        Fighter first = enemy("First", 40);
        Fighter second = enemy("Second", 40);

        EnemyAction action = behavior.choose(goblin, List.of(goblin), List.of(first, second)).orElseThrow();

        assertEquals(List.of(first), action.targets());
    }

    @Test void itAttacksEveryFoeWhenTheAttackIsOnAll()
    {
        Enemy boss = enemy("Boss", 40);
        boss.learn(e -> new EnemyAttack(e, "Roar", TargetType.ALL_ENEMIES, swing()));
        Fighter first = enemy("First", 40);
        Fighter second = enemy("Second", 40);

        EnemyAction action = behavior.choose(boss, List.of(boss), List.of(first, second)).orElseThrow();

        assertEquals(List.of(first, second), action.targets());
    }

    @Test void itHealsTheMostWoundedAlly()
    {
        Enemy shaman = enemy("Shaman", 40);
        shaman.learn(e -> new EnemyHeal(e, "Mend ally", TargetType.ONE_ALLY, 0.25));
        Enemy scratched = enemy("Scratched", 40);
        scratched.setHp(35);
        Enemy dying = enemy("Dying", 40);
        dying.setHp(5);

        EnemyAction action = behavior.choose(shaman, List.of(shaman, scratched, dying), List.of(enemy("Foe", 40))).orElseThrow();

        assertEquals(List.of(dying), action.targets());
    }

    @Test void itPassesWithoutAbilities()
    {
        Enemy empty = enemy("Empty", 40);

        assertEquals(Optional.empty(), behavior.choose(empty, List.of(empty), List.of(enemy("Foe", 40))));
    }

    @Test void itSkipsAnAbilityThatHasNobodyToUseItOn()
    {
        Enemy goblin = enemy("Goblin", 40);
        goblin.learn(e -> new EnemyAttack(e, "Club", TargetType.ONE_ENEMY, swing()));

        assertEquals(Optional.empty(), behavior.choose(goblin, List.of(goblin), List.of()));
    }
}