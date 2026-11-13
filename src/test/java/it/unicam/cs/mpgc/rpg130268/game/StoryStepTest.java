package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.game.StoryStep.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

// Each kind of step answers for itself, so GameFlow and Story never ask "what kind are you?"
class StoryStepTest
{
    private static final RandomGenerator ANY = () -> 0L;
    private static final Encounter WOLVES = () -> null;

    private static final StoryStep DIALOGUE = new DialogueStep(List.of(DialogueLine.narration("...")));
    private static final StoryStep BATTLE = new BattleStep(WOLVES);
    private static final StoryStep TREK = new TrekStep("forest", 3, 0.0, List.of());
    private static final StoryStep REWARD = new RewardStep(new Reward(List.of(new MemberReward("gunther", 0, 1, 0, 1, 0, 0))));
    private static final StoryStep DEFEAT = new DefeatStep();
    private static final StoryStep END = new EndStep("the end");

    @Test void onlyDialoguesAndTreksAreSafePoints()
    {
        assertTrue(DIALOGUE.isSafePoint());
        assertTrue(TREK.isSafePoint());

        assertFalse(BATTLE.isSafePoint());
        assertFalse(REWARD.isSafePoint());
        assertFalse(DEFEAT.isSafePoint());
        assertFalse(END.isSafePoint());
    }

    @Test void onlyTheEndStepIsTheEnd()
    {
        assertTrue(END.isEnd());

        for (StoryStep step : List.of(DIALOGUE, BATTLE, TREK, REWARD, DEFEAT))
            assertFalse(step.isEnd());
    }

    @Test void onlyRewardAndDefeatAreMadeByTheGame()
    {
        assertTrue(REWARD.isMadeByTheGame());
        assertTrue(DEFEAT.isMadeByTheGame());

        for (StoryStep step : List.of(DIALOGUE, BATTLE, TREK, END))
            assertFalse(step.isMadeByTheGame());
    }

    @Test void aTrekIsPastItsEndOnlyFromItsLength()
    {
        assertFalse(TREK.isPastEnd(2));
        assertTrue(TREK.isPastEnd(3));
        assertFalse(DIALOGUE.isPastEnd(99));
    }

    @Test void onlyABattleStepStartsABattle()
    {
        assertEquals(WOLVES, BATTLE.startingBattle().orElseThrow());
        assertTrue(DIALOGUE.startingBattle().isEmpty());
        assertTrue(TREK.startingBattle().isEmpty());
    }

    @Test void onlyATrekStepStartsAWalkFromWhereTheGameSays()
    {
        assertEquals(2, TREK.startWalk(2, ANY).orElseThrow().progress());
        assertTrue(DIALOGUE.startWalk(0, ANY).isEmpty());
        assertTrue(BATTLE.startWalk(0, ANY).isEmpty());
    }
}
