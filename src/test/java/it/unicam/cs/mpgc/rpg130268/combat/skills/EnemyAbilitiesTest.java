package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.damage.Scaling;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAttack;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyHeal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnemyAbilitiesTest {
    private final FakeCombatContext context = new FakeCombatContext();

    // strength 10, never crits: it has no luck
    private Enemy goblin() { return new Enemy("Goblin", new Stats(40, 10, 2, 3, 0)); }

    // defense 4: the defense lets 30/34 of the power through
    private Gunther gunther() {
        return new Gunther(new Stats(50, 8, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults());
    }

    private static final List<Hit> TWO_HITS = List.of(
            new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
            new Hit(300, ReactionOptions.PARRY_ONLY, StrikeWeight.HEAVY));

    private EnemyAttack clubSwing(Enemy user, TargetType targetType) {
        return new EnemyAttack(user, "Club swing", targetType, Scaling.of(ScalingStat.STRENGTH), TWO_HITS);
    }

    // ---------- Enemies don't use AP ----------

    @Test
    void enemyAbilitiesCostNothing() {
        EnemyAttack attack = clubSwing(goblin(), TargetType.ONE_ENEMY);

        assertEquals(0, attack.getApCost());
        assertTrue(attack.canPerform());
    }

    // ---------- EnemyAttack ----------

    @Test
    void attackHitsTheTargetWithTheWholeSequence() {
        Gunther g = gunther();

        clubSwing(goblin(), TargetType.ONE_ENEMY).perform(List.of(g), context);

        // strength 10: normal hit 10 -> 8.8, heavy hit 15 -> 13.2
        assertEquals(50 - (8 + 13), g.getHp());
        assertEquals(2, context.hitsResolved);
    }

    @Test
    void attackOnAllEnemiesHitsEveryLivingTarget() {
        Gunther first = gunther();
        Gunther second = gunther();

        clubSwing(goblin(), TargetType.ALL_ENEMIES).perform(List.of(first, second), context);

        assertEquals(50 - 21, first.getHp());
        assertEquals(50 - 21, second.getHp());
    }

    @Test
    void attackSkipsDeadTargets() {
        Gunther dead = gunther();
        dead.setHp(0);
        Gunther alive = gunther();

        clubSwing(goblin(), TargetType.ALL_ENEMIES).perform(List.of(dead, alive), context);

        assertEquals(0, dead.getHp());
        assertEquals(50 - 21, alive.getHp());
        assertEquals(2, context.hitsResolved);       // only the living target was attacked
    }

    @Test
    void attackStopsWhenTheTargetDiesInTheMiddleOfTheSequence() {
        Gunther fragile = gunther();
        fragile.setHp(5);                            // the first hit (8) kills

        clubSwing(goblin(), TargetType.ONE_ENEMY).perform(List.of(fragile), context);

        assertFalse(fragile.isAlive());
        assertEquals(1, context.hitsResolved);
    }

    @Test
    void attackCannotTargetItsUserWhenAimedAtEnemies() {
        Enemy g = goblin();

        assertThrows(IllegalArgumentException.class,
                () -> clubSwing(g, TargetType.ONE_ENEMY).perform(List.of(g), context));
    }

    @Test
    void attackNeedsScalingAndAtLeastOneHit() {
        Enemy g = goblin();

        assertThrows(NullPointerException.class,
                () -> new EnemyAttack(g, "Bad", TargetType.ONE_ENEMY, null, TWO_HITS));
        assertThrows(NullPointerException.class,
                () -> new EnemyAttack(g, "Bad", TargetType.ONE_ENEMY, Scaling.of(ScalingStat.STRENGTH), null));
        assertThrows(IllegalArgumentException.class,
                () -> new EnemyAttack(g, "Bad", TargetType.ONE_ENEMY, Scaling.of(ScalingStat.STRENGTH), List.of()));
    }

    @Test
    void attackKeepsItsOwnCopyOfTheSequence() {
        Gunther g = gunther();
        java.util.ArrayList<Hit> hits = new java.util.ArrayList<>(TWO_HITS);
        EnemyAttack attack = new EnemyAttack(goblin(), "Club swing", TargetType.ONE_ENEMY,
                Scaling.of(ScalingStat.STRENGTH), hits);

        hits.clear();                                // changing the original list changes nothing
        attack.perform(List.of(g), context);

        assertEquals(2, context.hitsResolved);
    }

    // ---------- EnemyHeal ----------

    @Test
    void healOnItselfRestoresAFractionOfTheMaxHealth() {
        Enemy shaman = new Enemy("Shaman", new Stats(40, 3, 2, 3, 0));
        shaman.setHp(10);

        new EnemyHeal(shaman, "Mend", TargetType.SELF, 0.25).perform(List.of(shaman), context);

        assertEquals(20, shaman.getHp());            // + 25% of 40
    }

    @Test
    void healOnItselfIsOnlyAvailableWhenWounded() {
        Enemy shaman = new Enemy("Shaman", new Stats(40, 3, 2, 3, 0));
        EnemyHeal mend = new EnemyHeal(shaman, "Mend", TargetType.SELF, 0.25);

        assertFalse(mend.canPerform());              // full health
        assertThrows(IllegalStateException.class, () -> mend.perform(List.of(shaman), context));

        shaman.setHp(39);
        assertTrue(mend.canPerform());
    }

    @Test
    void healNeverGoesAboveTheMaxHealth() {
        Enemy shaman = new Enemy("Shaman", new Stats(40, 3, 2, 3, 0));
        shaman.setHp(38);

        new EnemyHeal(shaman, "Mend", TargetType.SELF, 0.5).perform(List.of(shaman), context);

        assertEquals(40, shaman.getHp());
    }

    @Test
    void healOnAnAllyDoesNotNeedTheUserToBeWounded() {
        Enemy shaman = new Enemy("Shaman", new Stats(40, 3, 2, 3, 0));
        Enemy ally = goblin();
        ally.setHp(10);
        EnemyHeal heal = new EnemyHeal(shaman, "Mend ally", TargetType.ONE_ALLY, 0.25);

        assertTrue(heal.canPerform());               // the shaman is fine, but the ally is not
        heal.perform(List.of(ally), context);

        assertEquals(20, ally.getHp());              // + 25% of 40
        assertEquals(40, shaman.getHp());
    }

    @Test
    void healOnAllAlliesHealsEveryone() {
        Enemy shaman = new Enemy("Shaman", new Stats(40, 3, 2, 3, 0));
        Enemy ally = new Enemy("Imp", new Stats(20, 3, 2, 3, 0));
        shaman.setHp(10);
        ally.setHp(1);

        new EnemyHeal(shaman, "Chant", TargetType.ALL_ALLIES, 0.25).perform(List.of(shaman, ally), context);

        assertEquals(20, shaman.getHp());            // + 10
        assertEquals(6, ally.getHp());               // + 5
    }

    @Test
    void healRestoresAtLeastOnePoint() {
        Enemy tiny = new Enemy("Tiny", new Stats(3, 1, 1, 1, 0));
        tiny.setHp(1);

        new EnemyHeal(tiny, "Mend", TargetType.SELF, 0.1).perform(List.of(tiny), context);

        assertEquals(2, tiny.getHp());               // 10% of 3 is 0.3, rounded up to the minimum of 1
    }

    @Test
    void healCannotTargetEnemies() {
        Enemy shaman = new Enemy("Shaman", new Stats(40, 3, 2, 3, 0));

        assertThrows(IllegalArgumentException.class, () -> new EnemyHeal(shaman, "Bad", TargetType.ONE_ENEMY, 0.25));
        assertThrows(IllegalArgumentException.class, () -> new EnemyHeal(shaman, "Bad", TargetType.ALL_ENEMIES, 0.25));
    }

    @Test
    void healFractionMustBeAboveZeroAndNotOverOne() {
        Enemy shaman = new Enemy("Shaman", new Stats(40, 3, 2, 3, 0));

        assertThrows(IllegalArgumentException.class, () -> new EnemyHeal(shaman, "Bad", TargetType.SELF, 0));
        assertThrows(IllegalArgumentException.class, () -> new EnemyHeal(shaman, "Bad", TargetType.SELF, 1.1));
        assertDoesNotThrow(() -> new EnemyHeal(shaman, "Full", TargetType.SELF, 1.0));
    }
}