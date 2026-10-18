package it.unicam.cs.mpgc.rpg130268.combat.reaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactionOptionsTest
{
    @Test
    void parryOnlyAcceptsOnlyParry()
    {
        assertTrue(ReactionOptions.PARRY_ONLY.allows(ReactionType.PARRY));
        assertFalse(ReactionOptions.PARRY_ONLY.allows(ReactionType.DODGE));
    }

    @Test void dodgeOnlyAcceptsOnlyDodge()
    {
        assertTrue(ReactionOptions.DODGE_ONLY.allows(ReactionType.DODGE));
        assertFalse(ReactionOptions.DODGE_ONLY.allows(ReactionType.PARRY));
    }

    @Test void parryOrDodgeAcceptsBoth()
    {
        assertTrue(ReactionOptions.PARRY_OR_DODGE.allows(ReactionType.PARRY));
        assertTrue(ReactionOptions.PARRY_OR_DODGE.allows(ReactionType.DODGE));
    }

    @Test void noReactionIsNeverAllowed()
    {
        for (ReactionOptions options : ReactionOptions.values())
            assertFalse(options.allows(ReactionType.NONE));
    }
}