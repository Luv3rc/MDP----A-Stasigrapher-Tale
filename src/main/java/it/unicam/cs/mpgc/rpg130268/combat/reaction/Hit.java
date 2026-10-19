package it.unicam.cs.mpgc.rpg130268.combat.reaction;

import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;

import java.util.Objects;

// One strike of a combo.
// windUpMs and options are DATA: the UI reads them to know how long to show the strike
// before the impact and what the player can do about it. The domain never waits or sleeps.
// How hard the strike hits is decided only by the strike weight
public record Hit(long windUpMs, ReactionOptions options, StrikeWeight strikeWeight)
{
    public Hit
    {
        Objects.requireNonNull(options, "Options must be not null");
        Objects.requireNonNull(strikeWeight, "StrikeWeight must be not null");

        if (windUpMs < 0)
            throw new IllegalArgumentException("windUpMs must not be negative");
    }
}
