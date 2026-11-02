package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAttack;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyHeal;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.BasicAttack;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.CutsForInk;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.StasigraphicFury;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.ThirstForInk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbilityIdsTest
{
    private static List<Hit> swing() { return List.of(new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL)); }

    private static Enemy enemy() { return new Enemy("Enemy", new Stats(40, 5, 2, 3, 0)); }

    private static Gunther gunther() { return new Gunther(new Stats(100, 10, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults()); }

    @Test void theAbilitiesOfThePartyHaveTheirIdInTheClass()
    {
        assertEquals("basic-attack", BasicAttack.ID);
        assertEquals("cuts-for-ink", CutsForInk.ID);
        assertEquals("stasigraphic-fury", StasigraphicFury.ID);
        assertEquals("thirst-for-ink", ThirstForInk.ID);

        assertEquals(List.of(BasicAttack.ID, CutsForInk.ID, StasigraphicFury.ID, ThirstForInk.ID),
                gunther().getListOfAbility().stream().map(Ability::getId).toList());
    }

    @Test void anEnemyAbilityCanHaveItsIdChosenOnPurpose()
    {
        Enemy goblin = enemy();
        goblin.learn(e -> new EnemyAttack(e, "club-swing", "Heavy club swing", TargetType.ONE_ENEMY, swing()));
        goblin.learn(e -> new EnemyHeal(e, "mend", "Patch up", TargetType.SELF, 0.25));

        assertEquals("club-swing", goblin.getListOfAbility().get(0).getId());
        assertEquals("Heavy club swing", goblin.getListOfAbility().get(0).getName());
        assertEquals("mend", goblin.getListOfAbility().get(1).getId());
        assertEquals("Patch up", goblin.getListOfAbility().get(1).getName());
    }

    @Test void withoutAnIdTheNameGivesIt()
    {
        Enemy goblin = enemy();
        goblin.learn(e -> new EnemyAttack(e, "Club swing", TargetType.ONE_ENEMY, swing()));

        assertEquals("club-swing", goblin.getListOfAbility().getFirst().getId());
    }

    @Test void aBadIdIsRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> new EnemyAttack(enemy(), "Club swing", "Club swing", TargetType.ONE_ENEMY, swing()));
        assertThrows(NullPointerException.class, () -> new EnemyAttack(enemy(), null, "Club swing", TargetType.ONE_ENEMY, swing()));
    }

    @Test void anEnemyLearnsAnAbilityOnlyOnceAndTheIdIsWhatCounts()
    {
        Enemy goblin = enemy();
        goblin.learn(e -> new EnemyAttack(e, "club-swing", "Club swing", TargetType.ONE_ENEMY, swing()));
        goblin.learn(e -> new EnemyAttack(e, "club-swing", "Another name", TargetType.ONE_ENEMY, swing()));   // same id: already known
        goblin.learn(e -> new EnemyAttack(e, "quick-jab", "Club swing", TargetType.ONE_ENEMY, swing()));      // same name, other id: new

        assertEquals(List.of("club-swing", "quick-jab"), goblin.getListOfAbility().stream().map(Ability::getId).toList());
    }

    @Test void guntherLearnsAnAbilityOnlyOnce()
    {
        Gunther gunther = gunther();
        int known = gunther.getListOfAbility().size();

        gunther.learn(CutsForInk::new);

        assertEquals(known, gunther.getListOfAbility().size());
    }
}
