package it.unicam.cs.mpgc.rpg130268.game;

import java.util.Map;

/* *
    What the player did in a step:
    DialogueStep -> Finished
    BattleStep   -> BattleWon, BattleLost
    TrekStep     -> Walked (the player went one step further)
    (the steps that come after a battle, and their outcomes Retry and GoBack, come with GameFlow)
* */
public sealed interface StepOutcome
{
    record Finished() implements StepOutcome { }

    record Walked() implements StepOutcome { }

    // xpByMember is what the battle gave to each member of the party, by id of the member
    // (BattleEvent.BattleEnded has it, by member)
    record BattleWon(Map<String, Integer> xpByMember) implements StepOutcome
    {
        public BattleWon
        {
            xpByMember = Map.copyOf(xpByMember);

            if (xpByMember.values().stream().anyMatch(xp -> xp < 0))
                throw new IllegalArgumentException("the xp of a member must not be negative");
        }
    }

    record BattleLost() implements StepOutcome { }
}
