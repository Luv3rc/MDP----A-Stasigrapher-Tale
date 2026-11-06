package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.game.StoryStep.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoryTest
{
    private static final StoryNode END = new StoryNode("end", new EndStep("the end"));
    private static final StoryNode TALK = new StoryNode("talk", new DialogueStep(List.of(DialogueLine.narration("Once upon a time"))));

    @Test void aStoryNeedsSteps()
    {
        assertThrows(IllegalArgumentException.class, () -> new Story(List.of()));
    }

    @Test void aStoryMustFinishWithTheEnd()
    {
        assertThrows(IllegalArgumentException.class, () -> new Story(List.of(TALK)));
    }

    @Test void nothingCanComeAfterTheEnd()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new Story(List.of(END, new StoryNode("talk", TALK.step()), new StoryNode("end2", END.step()))));
    }

    @Test void theIdsCantRepeat()
    {
        assertThrows(IllegalArgumentException.class, () -> new Story(List.of(TALK, TALK, END)));
    }

    @Test void rewardAndDefeatAreMadeByTheGameAndCannotBeWrittenInTheStory()
    {
        Reward reward = new Reward(List.of(new MemberReward("gunther", 0, 1, 0, 1, 0, 0)));

        assertThrows(IllegalArgumentException.class, () -> new Story(List.of(new StoryNode("r", new RewardStep(reward)), END)));
        assertThrows(IllegalArgumentException.class, () -> new Story(List.of(new StoryNode("d", new DefeatStep()), END)));
    }

    @Test void theNodesAreCopied()
    {
        Story story = new Story(List.of(TALK, END));

        assertEquals(2, story.nodes().size());
        assertThrows(UnsupportedOperationException.class, () -> story.nodes().add(END));
    }

    @Test void aStepIsFoundByItsId()
    {
        Story story = new Story(List.of(TALK, END));

        assertEquals(0, story.indexOf("talk"));
        assertEquals(1, story.indexOf("end"));
        assertTrue(story.contains("end"));
        assertFalse(story.contains("nowhere"));
        assertThrows(IllegalArgumentException.class, () -> story.indexOf("nowhere"));
    }

    @Test void aNodeNeedsAnId()
    {
        assertThrows(IllegalArgumentException.class, () -> new StoryNode(" ", END.step()));
    }

    @Test void aDialogueWithoutBackgroundAndPartnerHasBlankOnes()
    {
        DialogueStep step = new DialogueStep(List.of(DialogueLine.narration("...")));

        assertEquals("", step.area());
        assertEquals("", step.partner());
        assertThrows(NullPointerException.class, () -> new DialogueStep(null, "", step.lines()));
        assertThrows(IllegalArgumentException.class, () -> new DialogueStep("road", "sariel", List.of()));
    }

    @Test void aDialogueLineWithoutSpeakerIsANarration()
    {
        assertTrue(DialogueLine.narration("It rains.").isNarration());
        assertFalse(new DialogueLine("Gunther", "Hello").isNarration());
        assertThrows(IllegalArgumentException.class, () -> new DialogueLine("Gunther", "  "));
    }

    // ---------- The trek: a walk through any place ----------

    @Test void aTrekIsAWalkThroughAPlaceAndTheEncountersOfThatPlace()
    {
        Encounter wolves = () -> null;
        TrekStep forest = new TrekStep("forest", 5, 0.5, List.of(wolves));

        assertEquals("forest", forest.area());
        assertEquals(5, forest.length());
        assertEquals(0.5, forest.encounterChance());
        assertEquals(List.of(wolves), forest.encounters());
    }

    @Test void aTrekNeedsAnAreaToShow()
    {
        assertThrows(IllegalArgumentException.class, () -> new TrekStep(" ", 5, 0.0, List.of()));
        assertThrows(NullPointerException.class, () -> new TrekStep(null, 5, 0.0, List.of()));
    }

    @Test void aTrekHasAtLeastOneStepAndAChanceBetweenZeroAndOne()
    {
        assertThrows(IllegalArgumentException.class, () -> new TrekStep("cave", 0, 0.0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new TrekStep("cave", 3, 1.5, List.of(() -> null)));
        assertThrows(IllegalArgumentException.class, () -> new TrekStep("cave", 3, -0.1, List.of(() -> null)));
    }

    @Test void aTrekWithAChanceNeedsSomethingToMeet()
    {
        assertThrows(IllegalArgumentException.class, () -> new TrekStep("cave", 3, 0.5, List.of()));
        assertDoesNotThrow(() -> new TrekStep("cave", 3, 0.0, List.of()));         // a quiet walk
    }
}
