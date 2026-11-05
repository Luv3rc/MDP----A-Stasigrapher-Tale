package it.unicam.cs.mpgc.rpg130268.game;

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

    // xpGained is what the battle gave to the party (it is in BattleEvent.BattleEnded)
    record BattleWon(int xpGained) implements StepOutcome
    {
        public BattleWon
        {
            if (xpGained < 0)
                throw new IllegalArgumentException("xpGained must not be negative");
        }
    }

    record BattleLost() implements StepOutcome { }
}
