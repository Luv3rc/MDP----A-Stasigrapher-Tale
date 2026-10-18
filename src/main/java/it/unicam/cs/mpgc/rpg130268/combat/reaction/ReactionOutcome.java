package it.unicam.cs.mpgc.rpg130268.combat.reaction;

// The result of one reaction. A parry is either PERFECT or MISS. A dodge can also be GOOD
public record ReactionOutcome(ReactionType type, ReactionQuality quality)
{
    public static ReactionOutcome noReaction() { return new ReactionOutcome(ReactionType.NONE, ReactionQuality.MISS); }

    // Perfect and Good dodges both avoid damage, difference is only feedback.
    // Could be used for accessories in future (e.g. an accessory that gives you
    // 1 AP on perfect dodges)
    public boolean avoidsDamage() { return quality != ReactionQuality.MISS; }

    public boolean isSuccessfulParry() { return (type == ReactionType.PARRY && quality == ReactionQuality.PERFECT); }
}
