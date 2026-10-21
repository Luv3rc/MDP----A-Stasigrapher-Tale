package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
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
        return new EnemyAttack(user, "Club swing", targetType, TWO_HITS);
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

        // Multiplicative Math: Unarmed (Base 1, Str D 0.5x).
        // Goblin Str 10 adds 1.0 (Innate) + 0.5 (Weapon) = 2.5x Total Multiplier.
        // Normal hit power = 2.5. Heavy hit power = 3.75.
        // Defense 4 lets 30/34 through -> 2.2 (2 damage) + 3.3 (3 damage) = 5 total damage.
        assertEquals(45, g.getHp());
        assertEquals(2, context.hitsResolved);
    }

    @Test
    void attackOnAllEnemiesHitsEveryLivingTarget() {
        Gunther first = gunther();
        Gunther second = gunther();

        clubSwing(goblin(), TargetType.ALL_ENEMIES).perform(List.of(first, second), context);

        assertEquals(45, first.getHp());
        assertEquals(45, second.getHp());
    }

    @Test
    void attackSkipsDeadTargets() {
        Gunther dead = gunther();
        dead.setHp(0);
        Gunther alive = gunther();

        clubSwing(goblin(), TargetType.ALL_ENEMIES).perform(List.of(dead, alive), context);

        assertEquals(0, dead.getHp());
        assertEquals(45, alive.getHp());
        assertEquals(2, context.hitsResolved);       // only the living target was attacked
    }

    @Test
    void attackStopsWhenTheTargetDiesInTheMiddleOfTheSequence()
    {
        Enemy goblin = goblin();
        goblin.equip(new Weapon("God Fists", WeaponType.UNARMED, 999, ScalingGrade.S));

        Gunther fragile = gunther();
        fragile.setHp(5);                            // the first hit instantly kills

        clubSwing(goblin, TargetType.ONE_ENEMY).perform(List.of(fragile), context);

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
                () -> new EnemyAttack(g, "Bad", TargetType.ONE_ENEMY, null));
        assertThrows(IllegalArgumentException.class,
                () -> new EnemyAttack(g, "Bad", TargetType.ONE_ENEMY, List.of()));
    }

    @Test
    void attackKeepsItsOwnCopyOfTheSequence() {
        Gunther g = gunther();
        java.util.ArrayList<Hit> hits = new java.util.ArrayList<>(TWO_HITS);
        EnemyAttack attack = new EnemyAttack(goblin(), "Club swing", TargetType.ONE_ENEMY, hits);

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

        assertTrue(heal.canPerform());               // the shaman is fine, but the allay is not
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

    // ---------- Enemies learn abilities ----------

    @Test
    void anEnemyStartsWithoutAbilities() {
        assertTrue(goblin().getListOfAbility().isEmpty());
    }

    @Test
    void anEnemyLearnsAbilitiesBuiltWithItselfAsTheUser() {
        Enemy shaman = new Enemy("Shaman", new Stats(40, 3, 2, 3, 0));
        shaman.setHp(10);

        shaman.learn(e -> new EnemyHeal(e, "Mend", TargetType.SELF, 0.25));
        shaman.learn(e -> clubSwing(e, TargetType.ONE_ENEMY));

        assertEquals(List.of("Mend", "Club swing"), shaman.getListOfAbility().stream().map(Ability::getName).toList());

        // The ability really belongs to the shaman: using it heals the shaman
        shaman.getListOfAbility().getFirst().perform(List.of(shaman), context);
        assertEquals(20, shaman.getHp());
    }

    @Test
    void learningTheSameAbilityTwiceKeepsTheFirstOne() {
        Enemy goblin = goblin();

        goblin.learn(e -> clubSwing(e, TargetType.ONE_ENEMY));
        goblin.learn(e -> clubSwing(e, TargetType.ALL_ENEMIES));   // same name

        assertEquals(1, goblin.getListOfAbility().size());
        assertEquals(TargetType.ONE_ENEMY, goblin.getListOfAbility().getFirst().getTargetType());
    }

    @Test
    void theListOfAbilitiesIsACopy() {
        Enemy goblin = goblin();
        goblin.learn(e -> clubSwing(e, TargetType.ONE_ENEMY));

        assertThrows(UnsupportedOperationException.class, () -> goblin.getListOfAbility().clear());
        assertEquals(1, goblin.getListOfAbility().size());
    }

    @Test
    void learningNeedsAFactoryThatBuildsSomething() {
        Enemy goblin = goblin();

        assertThrows(NullPointerException.class, () -> goblin.learn(null));
        assertThrows(NullPointerException.class, () -> goblin.learn(e -> null));
    }

    @Test
    void anAttackOnEveryoneIsOneAttackNotOnePerTarget() {
        Gunther first = gunther();
        Gunther second = gunther();

        clubSwing(goblin(), TargetType.ALL_ENEMIES).perform(List.of(first, second), context);

        assertEquals(1, context.attackTargets.size());
        assertEquals(List.of(first, second), context.attackTargets.getFirst());
    }
}