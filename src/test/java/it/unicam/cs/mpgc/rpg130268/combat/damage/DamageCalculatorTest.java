package it.unicam.cs.mpgc.rpg130268.combat.damage;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat.AGILITY;
import static it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat.LUCK;
import static it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat.STRENGTH;
import static org.junit.jupiter.api.Assertions.*;
import static it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight.*;


class DamageCalculatorTest
{
    private static class SequenceRandom implements RandomGenerator
    {
        private final double[] values;
        private int next = 0;
        SequenceRandom(double... values) { this.values = values; }

        @Override public double nextDouble() { return values[next++]; }
        @Override public long nextLong() { return 0; }
    }

    // Random source whose roll we decide
    private static class FixedRandom implements RandomGenerator
    {
        private final double value;
        FixedRandom(double value) { this.value = value; }

        @Override public double nextDouble() { return value; }
        @Override public long nextLong() { return 0; }
    }

    // Random source that fails the test if anyone rolls
    private static class ForbiddenRandom implements RandomGenerator
    {
        @Override public double nextDouble() { throw new AssertionError("nobody should roll"); }
        @Override public long nextLong() { throw new AssertionError("nobody should roll"); }
    }

    // Default rules: crit chance = luck * 0.5/60, at most 0.5; crit x2; damage taken = 30 / (30 + defense); min damage 1
    private DamageCalculator calculator(double roll)
    {
        return new DamageCalculator(DamageRules.defaults(), new FixedRandom(roll));
    }

    // strength 8, agility 6, luck 0: never crits
    private Enemy attacker() { return new Enemy("Attacker", new Stats(50, 8, 4, 6, 0)); }

    private Enemy target(int defense) { return new Enemy("Target", new Stats(100, 1, defense, 1, 0)); }

// ---------- Power: scaling, strike weight and bonus ----------

    @Test void powerIsTheScalingValueTimesTheWeightTimesTheBonus()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), HEAVY, 2.0);

        assertEquals(24, calculator(0.99).calculate(spec, attacker(), target(0)).amount());   // 8 * 1.5 * 2.0
    }

    @Test void bonusRaisesThePower()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL, 1.5);

        assertEquals(12, calculator(0.99).calculate(spec, attacker(), target(0)).amount());   // 8 * 1.5
    }

    @Test void theStrikeWeightChangesThePower()
    {
        DamageSpec light = new DamageSpec(Scaling.of(STRENGTH), LIGHT);
        DamageSpec extreme = new DamageSpec(Scaling.of(STRENGTH), EXTREME);

        assertEquals(6, calculator(0.99).calculate(light, attacker(), target(0)).amount());     // 8 * 0.75
        assertEquals(16, calculator(0.99).calculate(extreme, attacker(), target(0)).amount());  // 8 * 2.0
    }

    @Test void differentSkillsCanScaleOnDifferentStats()
    {
        DamageSpec onStrength = new DamageSpec(Scaling.of(STRENGTH), NORMAL);
        DamageSpec onAgility = new DamageSpec(Scaling.of(AGILITY), NORMAL);

        assertEquals(8, calculator(0.99).calculate(onStrength, attacker(), target(0)).amount());
        assertEquals(6, calculator(0.99).calculate(onAgility, attacker(), target(0)).amount());
    }

    @Test void furyStyleSpecTruncatesTheResult()
    {
        // agility 6 with an ink bonus of 1.4: 8.4 -> 8
        DamageSpec spec = new DamageSpec(Scaling.of(AGILITY), NORMAL, 1 + 4 / 10.0);

        assertEquals(8, calculator(0.99).calculate(spec, attacker(), target(0)).amount());
    }

    @Test void mixedScalingUsesBothStats()
    {
        // strength 8 and agility 6, half of each: 4 + 3 = 7
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH, 0.5).and(AGILITY, 0.5), NORMAL);

        assertEquals(7, calculator(0.99).calculate(spec, attacker(), target(0)).amount());
    }

    // ---------- Weapon damage ----------

    @Test void aFighterWithoutAWeaponAddsNothing()
    {
        assertEquals(0, attacker().getWeaponDamage());
    }

    @Test void weaponDamageIsAddedBeforeTheMultipliers()
    {
        Enemy armed = new Enemy("Armed", new Stats(50, 8, 4, 6, 0)) {
            @Override public int getWeaponDamage() { return 5; }
        };
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), HEAVY);

        // (weapon 5 + strength 8) * 1.5 hits
        assertEquals(19, calculator(0.99).calculate(spec, armed, target(0)).amount());
    }

    @Test void weaponDamageIsAlsoBoostedByTheMultiplier()
    {
        Enemy armed = new Enemy("Armed", new Stats(50, 8, 4, 6, 0)) {
            @Override public int getWeaponDamage() { return 5; }
        };
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL, 2.0);

        // (weapon 5 + strength 8) * 2.0
        assertEquals(26, calculator(0.99).calculate(spec, armed, target(0)).amount());
    }

    @Test void weaponDamageGivesPowerEvenToAnAttackerWithNoScalingStat()
    {
        Enemy armed = new Enemy("Armed", new Stats(50, 0, 4, 6, 0)) {
            @Override public int getWeaponDamage() { return 5; }
        };
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        assertEquals(5, calculator(0.99).calculate(spec, armed, target(0)).amount());
    }

    // ---------- Defense ----------

    @Test void defenseLetsOnlyAPartOfThePowerThrough()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL, 3.0);   // power 24

        assertEquals(12, calculator(0.99).calculate(spec, attacker(), target(30)).amount());   // 24 * 30/60
    }

    @Test void moreDefenseMeansLessDamage()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL, 3.0);

        int low = calculator(0.99).calculate(spec, attacker(), target(5)).amount();
        int high = calculator(0.99).calculate(spec, attacker(), target(50)).amount();

        assertTrue(high < low);
    }

    @Test void aHitThatLandsAlwaysDealsTheMinimumDamage()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);   // power 8 against huge defense

        assertEquals(1, calculator(0.99).calculate(spec, attacker(), target(1000)).amount());
    }

    @Test void minimumDamageComesFromTheRules()
    {
        DamageRules rules = new DamageRules(0.01, 0.5, 2.0, 30, 3);
        DamageCalculator custom = new DamageCalculator(rules, new FixedRandom(0.99));
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        assertEquals(3, custom.calculate(spec, attacker(), target(1000)).amount());
    }

    // ---------- Zero power ----------

    @Test void anAttackWithNoPowerDoesNothingAndDoesNotRoll()
    {
        Enemy weak = new Enemy("Weak", new Stats(50, 0, 4, 6, 0));   // strength 0
        DamageCalculator calculator = new DamageCalculator(DamageRules.defaults(), new ForbiddenRandom());
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        DamageResult result = calculator.calculate(spec, weak, target(0));

        assertEquals(0, result.amount());
        assertFalse(result.critical());
    }

    @Test void scalingOnAStatTheAttackerLacksGivesNoPower()
    {
        // luck 0 and a skill that scales only on luck
        DamageSpec spec = new DamageSpec(Scaling.of(LUCK), NORMAL, 2.0);

        assertEquals(0, calculator(0.0).calculate(spec, attacker(), target(0)).amount());
    }

    // ---------- Critical hits ----------

    // strength 10, luck 60: crit chance reaches its cap of 50%
    private Enemy lucky() { return new Enemy("Lucky", new Stats(50, 10, 4, 6, 60)); }

    @Test void aLowRollIsACriticalHitAndDoublesThePower()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        DamageResult result = calculator(0.1).calculate(spec, lucky(), target(0));

        assertTrue(result.critical());
        assertEquals(20, result.amount());
    }

    @Test void aHighRollIsAnOrdinaryHit()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        DamageResult result = calculator(0.9).calculate(spec, lucky(), target(0));

        assertFalse(result.critical());
        assertEquals(10, result.amount());
    }

    @Test void theCritChanceBoundaryIsExclusive()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        assertTrue(calculator(0.49).calculate(spec, lucky(), target(0)).critical());
        assertFalse(calculator(0.5).calculate(spec, lucky(), target(0)).critical());   // chance is 0.5
    }

    @Test void noLuckMeansNoCritsEvenWithTheLowestRoll()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        assertFalse(calculator(0.0).calculate(spec, attacker(), target(0)).critical());
    }

    @Test void moreLuckThanTheCapDoesNotRaiseTheChanceFurther()
    {
        Enemy veryLucky = new Enemy("VeryLucky", new Stats(50, 10, 4, 6, 500));
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        assertFalse(calculator(0.5).calculate(spec, veryLucky, target(0)).critical());
    }

    @Test void theCriticalMultiplierIsAppliedBeforeDefense()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        // power 10, critical: 20, then defense 30 lets half through: 10
        assertEquals(10, calculator(0.1).calculate(spec, lucky(), target(30)).amount());
    }

    @Test void everyHitOfAComboRollsItsOwnCrit()
    {
        // A combo calls the calculator once per hit: the first roll crits, the second one doesn't
        DamageCalculator combo = new DamageCalculator(DamageRules.defaults(), new SequenceRandom(0.1, 0.9));
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        DamageResult first = combo.calculate(spec, lucky(), target(0));
        DamageResult second = combo.calculate(spec, lucky(), target(0));

        assertTrue(first.critical());
        assertEquals(20, first.amount());     // 10 * 2
        assertFalse(second.critical());
        assertEquals(10, second.amount());
    }

    // ---------- Setup ----------

    @Test void calculatorKeepsItsRules()
    {
        DamageRules rules = DamageRules.defaults();
        assertSame(rules, new DamageCalculator(rules, new FixedRandom(0.5)).getDamageRules());
    }

    @Test void defaultCalculatorWorks()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        assertTrue(new DamageCalculator().calculate(spec, attacker(), target(0)).amount() >= 1);
    }

    @Test void missingPartsThrow()
    {
        DamageSpec spec = new DamageSpec(Scaling.of(STRENGTH), NORMAL);

        assertThrows(NullPointerException.class, () -> new DamageCalculator(null, new FixedRandom(0.5)));
        assertThrows(NullPointerException.class, () -> new DamageCalculator(DamageRules.defaults(), null));
        assertThrows(NullPointerException.class, () -> calculator(0.5).calculate(null, attacker(), target(0)));
        assertThrows(NullPointerException.class, () -> calculator(0.5).calculate(spec, null, target(0)));
        assertThrows(NullPointerException.class, () -> calculator(0.5).calculate(spec, attacker(), null));
    }
}