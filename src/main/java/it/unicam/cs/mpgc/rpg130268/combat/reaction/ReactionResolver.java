package it.unicam.cs.mpgc.rpg130268.combat.reaction;

import java.util.Objects;

/* *
    Compares the moment of the press with the moment of the impact.
    The windows aren't perfectly centered on the impact, but the player can react a little early or a little late.
    e.g. the parry in 160, but you can react 120ms earlier or 40ms later
    Parry: only works inside the parry window.
    Dodge: works inside the dodge window, and is PERFECT if it is also inside the parry window
*/
public class ReactionResolver
{
    private final ReactionWindows reactionWindows;

    public ReactionResolver(ReactionWindows reactionWindows)
    {
        this.reactionWindows = Objects.requireNonNull(reactionWindows, "reactionWindows must not be null");
    }

    public ReactionOutcome resolve(ReactionInput input, Hit hit)
    {
        if (input.type() == ReactionType.NONE)
            return ReactionOutcome.noReaction();

        // Reaction not accepted, count as miss
        if (!hit.options().allows(input.type()))
            return new ReactionOutcome(input.type(), ReactionQuality.MISS);

        long distance = input.timeMs() - hit.windUpMs();

        return switch (input.type())
        {
            case PARRY -> new ReactionOutcome(ReactionType.PARRY,
                    isWithinParry(distance) ? ReactionQuality.PERFECT : ReactionQuality.MISS);
            case DODGE -> new ReactionOutcome(ReactionType.DODGE, dodgeQuality(distance));
            case NONE -> ReactionOutcome.noReaction();
        };
    }

    private ReactionQuality dodgeQuality(long distance)
    {
        if (isWithinParry(distance)) return ReactionQuality.PERFECT;
        if (isWithinDodge(distance)) return ReactionQuality.GOOD;

        return ReactionQuality.MISS;
    }

    // Window of 3/4 before the impact and 1/4 after the impact
    private boolean isWithinParry(long delta)
    {
        return delta >= -reactionWindows.earlyParryWindowMs() && delta <= reactionWindows.lateParryWindowMs();
    }

    private boolean isWithinDodge(long delta)
    {
        return delta >= -reactionWindows.earlyDodgeWindowMs() && delta <= reactionWindows.lateDodgeWindowMs();
    }
}
