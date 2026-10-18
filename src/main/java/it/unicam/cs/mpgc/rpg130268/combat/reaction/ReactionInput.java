package it.unicam.cs.mpgc.rpg130268.combat.reaction;

import java.util.Objects;

public record ReactionInput(ReactionType type, long timeMs)
{
    public ReactionInput
    {
        Objects.requireNonNull(type, "type must not be null");
        if(timeMs < 0)
            throw new IllegalArgumentException("timeMs must not be negative");
    }

    public static ReactionInput none () { return new ReactionInput(ReactionType.NONE, 0); }
}
