package it.unicam.cs.mpgc.rpg130268.combat.reaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactionOutcomeTest
{
    @Test void noReactionAvoidsNothing()
    {
        ReactionOutcome out = ReactionOutcome.noReaction();

        assertFalse(out.avoidsDamage());
        assertFalse(out.isSuccessfulParry());
    }

    @Test void perfectParryIsASuccessfulParry()
    {
        ReactionOutcome out = new ReactionOutcome(ReactionType.PARRY, ReactionQuality.PERFECT);

        assertTrue(out.avoidsDamage());
        assertTrue(out.isSuccessfulParry());
    }

    @Test
    void missedParryIsNotSuccessful()
    {
        ReactionOutcome out = new ReactionOutcome(ReactionType.PARRY, ReactionQuality.MISS);

        assertFalse(out.avoidsDamage());
        assertFalse(out.isSuccessfulParry());
    }

    @Test void perfectAndGoodDodgesAvoidDamageButAreNotParries()
    {
        ReactionOutcome perfect = new ReactionOutcome(ReactionType.DODGE, ReactionQuality.PERFECT);
        ReactionOutcome good = new ReactionOutcome(ReactionType.DODGE, ReactionQuality.GOOD);

        assertTrue(perfect.avoidsDamage());
        assertTrue(good.avoidsDamage());
        assertFalse(perfect.isSuccessfulParry());
        assertFalse(good.isSuccessfulParry());
    }
}