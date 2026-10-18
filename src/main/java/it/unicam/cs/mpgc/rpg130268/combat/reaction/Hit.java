package it.unicam.cs.mpgc.rpg130268.combat.reaction;

import java.util.Objects;

public record Hit (long windUpMs, ReactionOptions options, double powerMultiplier)
{
    public Hit
    {
        Objects.requireNonNull(options, "Options must be not null");

        if(windUpMs < 0)
            throw new IllegalArgumentException("windUpMs must be greater than 0");

        if(powerMultiplier <= 0)
            throw new IllegalArgumentException("powerMultiplier must be greater than 0");
    }

    // For Hits that don't have a multiplier
    public Hit (long windUpMs, ReactionOptions options)
    {
        this(windUpMs, options, 1.0);
    }
}
