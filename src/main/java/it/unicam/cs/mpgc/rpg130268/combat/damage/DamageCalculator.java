package it.unicam.cs.mpgc.rpg130268.combat.damage;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;

import java.util.Objects;
import java.util.random.RandomGenerator;

/* *
    Decides how much damage an attack deals. It does NOT apply it! the caller does target.takeDamage(...)!

    1. base power = the attacker's stats weighted by the attack's Scaling * hits * multiplier
    2. critical hit: rolled once per attack, with a chance based on the attacker's luck. It multiplies the power
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

        double scalingValue = spec.scaling().valueFor(attacker.getStats());
        double power = (attacker.getWeaponDamage() + scalingValue) * spec.multiplier();

        // An attack with no power does nothing: no roll and no minimum damage
        if (power == 0) return new DamageResult(0, false);

        boolean critical = random.nextDouble() < damageRules.critChance(attacker.getStats().luck());
        if (critical) power *= damageRules.critMultiplier();

        double afterDefense = power * damageRules.damageTakenFactor(target.getStats().defense());
        return new DamageResult(Math.max(damageRules.minDamage(), (int) afterDefense), critical);
    }
}
