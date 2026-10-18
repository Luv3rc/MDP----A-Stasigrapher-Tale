package it.unicam.cs.mpgc.rpg130268.combat.damage;

// Rules for how damage works in combat
// - Luck is the chance of a critical hit: critChancePerLuck for every point of luck, up to maxCritChance
// ? - Defense reduces damage with diminishing returns: damage taken = defenseDampening / (defenseDampening + defense)
public record DamageRules(double critChancePerLuck, double maxCritChance, double critMultiplier, int defenseDampening, int minDamage)
{
    public static final int STAT_CAP = 60;

    public DamageRules
    {
        if (critChancePerLuck < 0)
            throw new IllegalArgumentException("critChancePerLuck must be greater than or equal to 0");

        if (maxCritChance < 0 || maxCritChance > 1)
            throw new IllegalArgumentException("maxCritChance must be greater than or equal to 0 and not over 1");

        if (critMultiplier <= 1)
            throw new IllegalArgumentException("critMultiplier must be greater than 1");

        if (defenseDampening <= 0)
            throw new IllegalArgumentException("defenseDampening must be greater than 0");

        if (minDamage < 1)
            throw new IllegalArgumentException("minDamage must be greater than or equal to 1");
    }

    public static DamageRules defaults()
    {
        return new DamageRules(critChancePerLuckToReach(0.5), 0.5, 2, 30, 1);
    }

    public static double critChancePerLuckToReach(double critChanceAtCap)
    {
        if (critChanceAtCap <= 0 || critChanceAtCap > 1)
            throw new IllegalArgumentException("critChanceAtCap must be greater than 0 and not over 1");

        return critChanceAtCap / STAT_CAP;
    }

    public double critChance(int luck) { return Math.min(maxCritChance, luck * critChancePerLuck); }

    // The part of the damage that goes through: 1.0 with no defense, never 0
    public double damageTakenFactor(int defense) { return (double)defenseDampening / (defenseDampening + defense); }
}