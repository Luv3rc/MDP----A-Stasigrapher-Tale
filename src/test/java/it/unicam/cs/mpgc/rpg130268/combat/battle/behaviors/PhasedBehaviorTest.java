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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PhasedBehaviorTest
{
    private static List<Hit> swing() { return List.of(new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL)); }

    // 40 HP, and an attack for every phase
    private Enemy boss()
    {
        Enemy boss = new Enemy("Boss", new Stats(40, 5, 2, 3, 0));
        boss.learn(e -> new EnemyAttack(e, "Calm", TargetType.ONE_ENEMY, swing()));
        boss.learn(e -> new EnemyAttack(e, "Angry", TargetType.ONE_ENEMY, swing()));
        boss.learn(e -> new EnemyAttack(e, "Desperate", TargetType.ONE_ENEMY, swing()));
        return boss;
    }

    private final Fighter foe = new Enemy("Foe", new Stats(40, 5, 2, 3, 0));

    // A behavior that always uses the ability with this name
    private static EnemyBehavior using(String name)
    {
        return (self, allies, foes) -> EnemyTargeting.abilityNamed(self, name)
                .flatMap(ability -> EnemyTargeting.actionFor(self, ability, allies, foes));
    }

    private String choose(PhasedBehavior behavior, Enemy boss)
    {
        Optional<EnemyAction> action = behavior.choose(boss, List.of(boss), List.of(foe));
        return action.orElseThrow().ability().getName();
    }

    @Test void aHealthyEnemyFollowsTheFirstBehavior()
    {
        PhasedBehavior behavior = new PhasedBehavior(using("Calm")).below(0.5, using("Angry"));

        assertEquals("Calm", choose(behavior, boss()));
    }

    @Test void whenHalfOfItsHealthIsGoneItChanges()
    {
        PhasedBehavior behavior = new PhasedBehavior(using("Calm")).below(0.5, using("Angry"));
        Enemy boss = boss();

        boss.setHp(21);                                  // just above half
        assertEquals("Calm", choose(behavior, boss));

        boss.setHp(20);                                  // exactly half: the phase has begun
        assertEquals("Angry", choose(behavior, boss));
    }

    @Test void withSeveralPhasesTheLowestOneThatAppliesWins()
    {
        PhasedBehavior behavior = new PhasedBehavior(using("Calm"))
                .below(0.5, using("Angry"))
                .below(0.2, using("Desperate"));
        Enemy boss = boss();

        boss.setHp(40);
        assertEquals("Calm", choose(behavior, boss));
        boss.setHp(20);
        assertEquals("Angry", choose(behavior, boss));
        boss.setHp(9);                                   // 22%: not yet desperate
        assertEquals("Angry", choose(behavior, boss));
        boss.setHp(8);                                   // 20%
        assertEquals("Desperate", choose(behavior, boss));
    }

    @Test void thePhasesCanBeAddedInAnyOrder()
    {
        PhasedBehavior behavior = new PhasedBehavior(using("Calm"))
                .below(0.2, using("Desperate"))
                .below(0.5, using("Angry"));
        Enemy boss = boss();

        boss.setHp(15);

        assertEquals("Angry", choose(behavior, boss));
    }

    @Test void thePhasesMustMakeSense()
    {
        assertThrows(NullPointerException.class, () -> new PhasedBehavior(null));
        assertThrows(NullPointerException.class, () -> new PhasedBehavior(using("Calm")).below(0.5, null));
        assertThrows(IllegalArgumentException.class, () -> new PhasedBehavior(using("Calm")).below(0, using("Angry")));
        assertThrows(IllegalArgumentException.class, () -> new PhasedBehavior(using("Calm")).below(1, using("Angry")));
        assertThrows(IllegalArgumentException.class, () -> new PhasedBehavior(using("Calm")).below(-0.5, using("Angry")));
    }
}