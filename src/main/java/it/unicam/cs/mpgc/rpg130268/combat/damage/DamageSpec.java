package it.unicam.cs.mpgc.rpg130268.combat.damage;

import java.util.Objects;

// Describes an attack, without saying who attacks or who is hit
// Base power = scaling value * hits * multiplier (this does not take into consideration defense for who gets hit)
// e.g. Cuts for Ink      -> new DamageSpec(Scaling.of(STRENGTH), 1.0)
//      Stasigraphic Fury -> new DamageSpec(Scaling.of(AGILITY), 1 + ink / 10.0)
//      General hit       -> new DamageSpec(Scaling.of(STAT).and(OTHER_STAT), hit.powerMultiplier())
// Note: Hits have been removed. Since a combo of N hits calls the DamageCalculator N times
// I thought that having the number of hits in the DamageSpec was not correct and, moreover, useless
public record DamageSpec(Scaling scaling, double multiplier)
{
    public DamageSpec
    {
        Objects.requireNonNull(scaling, "Scaling must not be null");

        if (multiplier <= 0)
            throw new IllegalArgumentException("multiplier must be greater than 0");
    }
}
