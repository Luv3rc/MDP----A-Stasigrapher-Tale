package it.unicam.cs.mpgc.rpg130268.combat.damage;

// These are the scaling a Weapon can have
// Scale is directed to the Stats (e.g. S scale on Luck)
public enum ScalingGrade
{
    S(1.75),
    A(1.2),
    B(1.0),
    C(0.75),
    D(0.5),
    NONE(0.0);

    private final double multiplier;

    ScalingGrade(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }
}