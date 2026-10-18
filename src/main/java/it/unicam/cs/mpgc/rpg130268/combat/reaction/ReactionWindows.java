package it.unicam.cs.mpgc.rpg130268.combat.reaction;

// How long the player has to react.
// The same for every attack, so the rhythm is learnable
public record ReactionWindows(long parryWindowMs, long dodgeWindowMs)
{
    public ReactionWindows
    {
        if (parryWindowMs <= 0)
            throw new IllegalArgumentException("Parry Window can't be less or equal to 0ms");

        if (dodgeWindowMs <= parryWindowMs)
            throw new IllegalArgumentException("Dodge Window can't be less or equal to the parry window");
    }

    public static ReactionWindows defaults() { return new ReactionWindows(160, 300); }
}
