package it.unicam.cs.mpgc.rpg130268.combat.damage;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.random.RandomGenerator;

import static it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat.AGILITY;
import static it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat.STRENGTH;
import static it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight.*;
import static org.junit.jupiter.api.Assertions.*;

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

    private static class FixedRandom implements RandomGenerator
    {
        private final double value;
        FixedRandom(double value) { this.value = value; }

        @Override public double nextDouble() { return value; }
        @Override public long nextLong() { return 0; }
    }

    private static class ForbiddenRandom implements RandomGenerator
    {
        @Override public double nextDouble() { throw new AssertionError("nobody should roll"); }
        @Override public long nextLong() { throw new AssertionError("nobody should roll"); }
    }

    // A dummy fighter that is allowed to equip every single weapon type for easy testing
    private static class TestFighter extends Fighter
    {
        TestFighter(String name, Stats stats)
        {
            super(name, stats, java.util.EnumSet.allOf(WeaponType.class));
        }
    }

    private DamageCalculator calculator(double roll)
    {
        return new DamageCalculator(DamageRules.defaults(), new FixedRandom(roll));
    }

    // Base testing attacker: 20 Strength, 10 Agility, 0 Luck (Never crits by default)
    private TestFighter attacker()
    {
        return new TestFighter("Attacker", new Stats(50, 20, 4, 10, 0));
    }

    // Base testing target
    private TestFighter target(int defense)
    {
        return new TestFighter("Target", new Stats(100, 1, defense, 1, 0));
    }


    // ---------- Multiplicative Scaling & Weapon Power ----------

    @Test void innateStrengthActsAsAUniversalMultiplier()
    {
        TestFighter attacker = attacker();
        // Weapon with NO scaling. Base damage 10.
        // Multiplier = 1.0 (Base) + 2.0 (Innate 20 Str / 10) = 3.0x
        attacker.equip(new Weapon("Stick", WeaponType.CLUB, 10, Scaling.none()));

        DamageSpec spec = new DamageSpec(NORMAL);
        assertEquals(30, calculator(0.99).calculate(spec, attacker, target(0)).amount()); // 10 * 3.0
    }

    @Test void weaponScalingAddsAnExtraMultiplier()
    {
        TestFighter attacker = attacker();
        // Multiplier = 1.0 (Base) + 2.0 (Innate Str) + 3.5 (20 Str * 1.75 / 10) = 6.5x
        attacker.equip(new Weapon("Greatsword", WeaponType.SWORD, 10, ScalingGrade.S));

        DamageSpec spec = new DamageSpec(NORMAL);
        assertEquals(65, calculator(0.99).calculate(spec, attacker, target(0)).amount());
    }
    @Test void abilitiesCanScaleOnDifferentStatsViaTheWeapon()
    {
        TestFighter attacker = attacker();
        // Weapon with Agility scaling Grade A (1.2x)
        // Multiplier = 1.0 (Base) + 2.0 (Innate Str) + 1.2 (10 Agi * 1.2 / 10) = 4.2x
        attacker.equip(new Weapon("Dagger", WeaponType.DAGGER, 10, ScalingGrade.NONE, AGILITY, ScalingGrade.A));

        DamageSpec spec = new DamageSpec(NORMAL);
        assertEquals(42, calculator(0.99).calculate(spec, attacker, target(0)).amount()); // 10 * 4.2
    }

    @Test void mixedScalingCombinesBothStatsMultipliers()
    {
        TestFighter attacker = attacker();
        // Strength Grade C (0.75x) AND Agility Grade A (1.2x)
        // Str Scale: 20 * 0.75 / 10 = +1.5x
        // Agi Scale: 10 * 1.2 / 10 = +1.2x
        // Total Multiplier = 1.0 (Base) + 2.0 (Innate) + 1.5 + 1.2 = 5.7x
        attacker.equip(new Weapon("Assassin Blade", WeaponType.DAGGER, 10, ScalingGrade.C, AGILITY, ScalingGrade.A));

        DamageSpec spec = new DamageSpec(NORMAL);
        assertEquals(57, calculator(0.99).calculate(spec, attacker, target(0)).amount()); // 10 * 5.7
    }

    @Test void defaultUnarmedCombatStillProvidesBaselineDamage()
    {
        TestFighter attacker = attacker();
        // Unarmed defaults to Base 1, Strength Grade D (0.5x)
        // Total Mult = 1.0 (Base) + 2.0 (Innate) + 1.0 (20 * 0.5 / 10) = 4.0x
        // Power = 1 * 4.0 = 4

        DamageSpec spec = new DamageSpec(NORMAL);
        assertEquals(4, calculator(0.99).calculate(spec, attacker, target(0)).amount());
    }


    // ---------- Strike Weights and Dynamic Bonuses ----------

    @Test void strikeWeightMultipliesTheFinalPower()
    {
        TestFighter attacker = attacker();
        attacker.equip(new Weapon("Stick", WeaponType.CLUB, 10, Scaling.none())); // 30 Power

        DamageSpec light = new DamageSpec(LIGHT);
        DamageSpec extreme = new DamageSpec(EXTREME);

        assertEquals(22, calculator(0.99).calculate(light, attacker, target(0)).amount());    // 30 * 0.75 = 22.5 -> 22
        assertEquals(60, calculator(0.99).calculate(extreme, attacker, target(0)).amount());  // 30 * 2.0 = 60
    }

    @Test void bonusMultipliesTheFinalPower()
    {
        TestFighter attacker = attacker();
        attacker.equip(new Weapon("Stick", WeaponType.CLUB, 10, Scaling.none())); // 30 Power

        DamageSpec spec = new DamageSpec(NORMAL, 1.5); // +50% dynamic bonus (e.g. from Ink)
        assertEquals(45, calculator(0.99).calculate(spec, attacker, target(0)).amount());     // 30 * 1.5 = 45
    }


    // ---------- Defense Rules ----------

    @Test void defenseLetsOnlyAPartOfThePowerThrough()
    {
        TestFighter attacker = attacker();
        attacker.equip(new Weapon("Stick", WeaponType.CLUB, 10, Scaling.none())); // 30 Power

        // Target with 30 defense. Formula: 30 / (30 + 30) = 0.5 damage taken factor
        assertEquals(15, calculator(0.99).calculate(new DamageSpec(NORMAL), attacker, target(30)).amount());
    }

    @Test void moreDefenseMeansLessDamage()
    {
        TestFighter attacker = attacker();
        attacker.equip(new Weapon("Stick", WeaponType.CLUB, 10, Scaling.none()));

        int lowDefDamage = calculator(0.99).calculate(new DamageSpec(NORMAL), attacker, target(5)).amount();
        int highDefDamage = calculator(0.99).calculate(new DamageSpec(NORMAL), attacker, target(50)).amount();

        assertTrue(highDefDamage < lowDefDamage);
    }

    @Test void aHitThatLandsAlwaysDealsTheMinimumDamage()
    {
        TestFighter attacker = attacker();
        attacker.equip(new Weapon("Stick", WeaponType.CLUB, 10, Scaling.none()));

        // Target has massive defense, math results in < 1 damage
        assertEquals(1, calculator(0.99).calculate(new DamageSpec(NORMAL), attacker, target(1000)).amount());
    }


    // ---------- Zero Power Edge Cases ----------

    @Test void anAttackWithNoBaseDamageDoesNothingAndDoesNotRoll()
    {
        TestFighter attacker = attacker();
        attacker.equip(new Weapon("Broken", WeaponType.UNARMED, 0, ScalingGrade.S));

        DamageCalculator noRollCalc = new DamageCalculator(DamageRules.defaults(), new ForbiddenRandom());
        DamageResult result = noRollCalc.calculate(new DamageSpec(NORMAL), attacker, target(0));

        assertEquals(0, result.amount());
        assertFalse(result.critical());
    }


    // ---------- Critical Hits ----------

    // 20 Strength, 60 Luck (Hits the 50% max crit chance cap)
    private TestFighter luckyAttacker()
    {
        TestFighter attacker = new TestFighter("Lucky", new Stats(50, 20, 4, 10, 60));
        attacker.equip(new Weapon("Stick", WeaponType.CLUB, 10, Scaling.none())); // 30 Power
        return attacker;
    }

    @Test void aLowRollIsACriticalHitAndDoublesThePower()
    {
        DamageResult result = calculator(0.1).calculate(new DamageSpec(NORMAL), luckyAttacker(), target(0));

        assertTrue(result.critical());
        assertEquals(60, result.amount()); // 30 Power * 2.0 Crit Multiplier
    }

    @Test void aHighRollIsAnOrdinaryHit()
    {
        DamageResult result = calculator(0.9).calculate(new DamageSpec(NORMAL), luckyAttacker(), target(0));

        assertFalse(result.critical());
        assertEquals(30, result.amount());
    }

    @Test void theCritChanceBoundaryIsExclusive()
    {
        // 60 luck = 0.5 chance
        assertTrue(calculator(0.49).calculate(new DamageSpec(NORMAL), luckyAttacker(), target(0)).critical());
        assertFalse(calculator(0.5).calculate(new DamageSpec(NORMAL), luckyAttacker(), target(0)).critical());
    }

    @Test void everyHitOfAComboRollsItsOwnCrit()
    {
        DamageCalculator combo = new DamageCalculator(DamageRules.defaults(), new SequenceRandom(0.1, 0.9));

        DamageResult first = combo.calculate(new DamageSpec(NORMAL), luckyAttacker(), target(0));
        DamageResult second = combo.calculate(new DamageSpec(NORMAL), luckyAttacker(), target(0));

        assertTrue(first.critical());
        assertEquals(60, first.amount());

        assertFalse(second.critical());
        assertEquals(30, second.amount());
    }


    // ---------- Setup and Exceptions ----------

    @Test void missingPartsThrow()
    {
        assertThrows(NullPointerException.class, () -> new DamageCalculator(null, new FixedRandom(0.5)));
        assertThrows(NullPointerException.class, () -> new DamageCalculator(DamageRules.defaults(), null));
        assertThrows(NullPointerException.class, () -> calculator(0.5).calculate(null, attacker(), target(0)));
        assertThrows(NullPointerException.class, () -> calculator(0.5).calculate(new DamageSpec(NORMAL), null, target(0)));
        assertThrows(NullPointerException.class, () -> calculator(0.5).calculate(new DamageSpec(NORMAL), attacker(), null));
    }
}