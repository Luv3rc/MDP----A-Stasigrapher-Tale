package it.unicam.cs.mpgc.rpg130268.combat.damage;

// How hard one strike of a combo hits. It is the only "strength knob" of a hit:
// the multiplier is used by the DamageCalculator through DamageSpec.getTotalMultiplier()
public enum StrikeWeight
{
    LIGHT(0.75),
    NORMAL(1.00),
    HEAVY(1.50),
    EXTREME(2.00);

    private final double multiplier;

    StrikeWeight(double multiplier) { this.multiplier = multiplier; }

    public double getMultiplier() { return multiplier; }
}
