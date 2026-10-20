package it.unicam.cs.mpgc.rpg130268.combat.damage;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;

import java.util.Objects;
import java.util.random.RandomGenerator;

/* *
    Decides how much damage an attack deals. It does NOT apply it! the caller does target.takeDamage(...)!

    1. base power = Weapon Base * Total Stat Multiplier * Strike Weight * Dynamic Bonus
    2. critical hit: rolled at EVERY call (so every hit of a combo has its own roll), with a chance based on the
       attacker's luck. It multiplies the power
    3. the target's defense lets only a part of the power through (see DamageRules.damageTakenFactor),
       the result is truncated and never below minDamage

    Has no state besides the rules and the random source, so one instance can be shared by the whole combat.
    The random source can be replaced, so the tests can decide the outcome of the roll
*/
public class DamageCalculator
{
    private final DamageRules damageRules;
    private final RandomGenerator random;

    public DamageCalculator() { this(DamageRules.defaults(), RandomGenerator.getDefault()); }

    public DamageCalculator(DamageRules damageRules, RandomGenerator random)
    {
        this.damageRules = Objects.requireNonNull(damageRules, "damageRules must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public DamageRules getDamageRules() { return damageRules; }

    public DamageResult calculate(DamageSpec spec, Fighter attacker, Fighter target)
    {
        Objects.requireNonNull(spec, "spec must not be null");
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(target, "target must not be null");

        int weaponBase = attacker.getWeapon().baseDamage();

        // Strength multiplier (divided by 10 to turn stats into a percentage bonus)
        double innateStrengthMult = attacker.getStats().strength() / 10.0;

        // Weapon specific scaling multiplier (directly from the weapon, NOT the spec)
        double weaponScalingMult = attacker.getWeapon().scaling().valueFor(attacker.getStats()) / 10.0;

        // Base * (1.0 + Stats) * Strike Weight -- the +1 is to avoid 0
        double totalStatMultiplier = 1.0 + innateStrengthMult + weaponScalingMult;
        double power = weaponBase * totalStatMultiplier * spec.getTotalMultiplier();

        if (power == 0) return new DamageResult(0, false);

        boolean critical = random.nextDouble() < damageRules.critChance(attacker.getStats().luck());
        if (critical) power *= damageRules.critMultiplier();

        double afterDefense = power * damageRules.damageTakenFactor(target.getStats().defense());
        return new DamageResult(Math.max(damageRules.minDamage(), (int) afterDefense), critical);
    }
}
