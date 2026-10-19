package it.unicam.cs.mpgc.rpg130268.combat.damage;

import java.util.Objects;

// Describes ONE strike, without saying who attacks or who is hit
// Base power = (weapon damage + scaling value) * strike weight * bonus multiplier
// The defense of who gets hit is not part of it: the DamageCalculator applies it afterward
// e.g. Cuts for Ink      -> new DamageSpec(Scaling.of(STRENGTH), NORMAL)
//      Stasigraphic Fury -> new DamageSpec(Scaling.of(AGILITY), LIGHT, 1 + ink / 10.0)
//      General hit       -> new DamageSpec(Scaling.of(STAT).and(OTHER_STAT), StrikeWeight, N)
// Note: Hits have been removed. A combo of N hits calls the DamageCalculator N times (one spec per hit),
// so every hit has its own critical roll. Having the number of hits in the DamageSpec was not correct
public record DamageSpec(Scaling scaling, StrikeWeight weight, double bonusMultiplier)
{

    public DamageSpec
    {
        Objects.requireNonNull(scaling, "Scaling must not be null");
        Objects.requireNonNull(weight, "StrikeWeight must not be null");
        if (bonusMultiplier <= 0)
            throw new IllegalArgumentException("Bonus multiplier must be greater than 0");
    }

    // Constructor for standard attacks with NO dynamic buffs (bonus is 1.0)
    public DamageSpec(Scaling scaling, StrikeWeight weight)
    {
        this(scaling, weight, 1.0);
    }

    // The calculator will call this to get the true mathematical multiplier
    public double getTotalMultiplier() { return weight.getMultiplier() * bonusMultiplier; }
}
