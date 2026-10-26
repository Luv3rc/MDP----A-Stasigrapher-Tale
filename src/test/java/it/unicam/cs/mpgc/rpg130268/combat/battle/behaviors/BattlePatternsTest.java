package it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.battle.*;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageRules;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAttack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

// The patterns in a whole battle: the enemy really does what its behavior says, turn after turn
class BattlePatternTest
{
    private static class NoCrits implements RandomGenerator
    {
        @Override public double nextDouble() { return 0.99; }
        @Override public long nextLong() { return 0; }
    }

    // Strong enough to last: the patterns are what is being looked at, not who wins
    private Gunther gunther()
    {
        Gunther gunther = new Gunther(new Stats(500, 10, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults());
        gunther.equip(new Weapon("Test sword", WeaponType.SWORD, 10, ScalingGrade.NONE));
        return gunther;
    }

    // "One hit" has one hit and "Two hits" has two
    private Enemy goblin(int hp)
    {
        Enemy goblin = new Enemy("Goblin", new Stats(hp, 10, 2, 3, 0), 2, 30);
        goblin.equip(new Weapon("Club", WeaponType.UNARMED, 10, ScalingGrade.NONE));
        goblin.learn(e -> new EnemyAttack(e, "One hit", TargetType.ONE_ENEMY, List.of(
                new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL))));
        goblin.learn(e -> new EnemyAttack(e, "Two hits", TargetType.ONE_ENEMY, List.of(
                new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                new Hit(300, ReactionOptions.PARRY_ONLY, StrikeWeight.NORMAL))));
        return goblin;
    }

    private Battle battle(Gunther gunther, Enemy goblin, EnemyBehavior behavior)
    {
        LiveBattleContext live = new LiveBattleContext(new DamageCalculator(DamageRules.defaults(), new NoCrits()),
                ReactionWindows.defaults(), BattleRules.defaults(), new BattleClock());

        return new Battle(List.of(gunther), List.of(goblin), live, behavior);
    }

    // The player attacks every turn and never reacts; how many hits has each attack of the enemy, in order?
    private List<Integer> hitsOfTheAttacks(Battle battle, Gunther gunther, Enemy goblin, int attacks)
    {
        Ability basic = gunther.getListOfAbility().stream().filter(a -> a.getName().equals("Basic Attack")).findFirst().orElseThrow();
        List<Integer> hits = new ArrayList<>();

        battle.start();

        for (int frames = 0; hits.size() < attacks && battle.getPhase() != BattlePhase.OVER && frames < 20_000; frames++)
        {
            if (battle.getPhase() == BattlePhase.WAITING_FOR_PLAYER)
                battle.perform(basic, List.of(goblin));

            battle.update(10);

            for (BattleEvent event : battle.drainEvents())
                if (event instanceof BattleEvent.AttackStarted started)
                    hits.add(started.hits().size());
        }

        return hits;
    }

    @Test void anEnemyFollowsItsRotationTurnAfterTurn()
    {
        Gunther g = gunther();
        Enemy goblin = goblin(1000);
        EnemyBehavior behavior = new PerEnemyBehavior(new FirstUsableAbility())
                .assign(goblin, new RotationBehavior(List.of("Two hits", "One hit")));

        assertEquals(List.of(2, 1, 2, 1), hitsOfTheAttacks(battle(g, goblin, behavior), g, goblin, 4));
    }

    @Test void aBossChangesItsAttacksWhenItIsHurt()
    {
        Gunther g = gunther();
        Enemy boss = goblin(40);                         // Gunther hits for 18: 40 -> 22 (55%) -> 4 (10%)
        EnemyBehavior behavior = new PerEnemyBehavior(new FirstUsableAbility())
                .assign(boss, new PhasedBehavior(new RotationBehavior(List.of("One hit")))
                        .below(0.5, new RotationBehavior(List.of("Two hits"))));

        assertEquals(List.of(1, 2), hitsOfTheAttacks(battle(g, boss, behavior), g, boss, 2));
    }

    @Test void anEnemyWithoutAPatternJustUsesItsFirstAbility()
    {
        Gunther g = gunther();
        Enemy goblin = goblin(1000);

        assertEquals(List.of(1, 1, 1), hitsOfTheAttacks(battle(g, goblin, new PerEnemyBehavior(new FirstUsableAbility())), g, goblin, 3));
    }
}