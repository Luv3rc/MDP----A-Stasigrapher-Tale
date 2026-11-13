package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.game.DialogueLine;
import it.unicam.cs.mpgc.rpg130268.game.MemberReward;
import it.unicam.cs.mpgc.rpg130268.game.Reward;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StepHandlersTest
{
    private static final DialogueStep TALK = new DialogueStep(List.of(DialogueLine.narration("...")));
    private static final EndStep END = new EndStep("the end");
    private static final Reward REWARD = new Reward(List.of(new MemberReward("gunther", 0, 1, 0, 1, 0, 0)));

    // Handlers for every kind of step, each one answering with the name of the kind
    private static StepHandlers<String> everything()
    {
        return new StepHandlers<String>()
                .on(DialogueStep.class, step -> "dialogue")
                .on(BattleStep.class, step -> "battle")
                .on(TrekStep.class, step -> "trek")
                .on(RewardStep.class, step -> "reward")
                .on(DefeatStep.class, step -> "defeat")
                .on(EndStep.class, step -> "end");
    }

    @Test void everyKindOfStepGetsTheAnswerOfItsOwnHandler()
    {
        StepHandlers<String> handlers = everything();

        assertEquals("dialogue", handlers.handle(TALK));
        assertEquals("end", handlers.handle(END));
        assertEquals("defeat", handlers.handle(new DefeatStep()));
        assertEquals("reward", handlers.handle(new RewardStep(REWARD)));
    }

    @Test void theHandlerGetsTheStepAlreadyOfItsOwnKind()
    {
        StepHandlers<String> handlers = new StepHandlers<String>().on(EndStep.class, step -> step.message());

        assertEquals("the end", handlers.handle(END));
    }

    @Test void aStepWithNoHandlerSaysWhichKindItIs()
    {
        StepHandlers<String> handlers = new StepHandlers<String>().on(EndStep.class, step -> "end");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> handlers.handle(TALK));

        assertTrue(error.getMessage().contains("DialogueStep"));
    }

    @Test void aKindOfStepHasOnlyOneHandler()
    {
        StepHandlers<String> handlers = new StepHandlers<String>().on(EndStep.class, step -> "end");

        assertThrows(IllegalArgumentException.class, () -> handlers.on(EndStep.class, step -> "another"));
    }

    @Test void theKindsWithNoHandlerAreKnown()
    {
        StepHandlers<String> handlers = new StepHandlers<String>().on(EndStep.class, step -> "end").on(DialogueStep.class, step -> "dialogue");

        assertEquals(java.util.Set.of(BattleStep.class, TrekStep.class, RewardStep.class, DefeatStep.class), handlers.missing());
    }

    @Test void requireAllAcceptsHandlersForEveryKindOfStep()
    {
        assertTrue(everything().missing().isEmpty());
        assertDoesNotThrow(() -> everything().requireAll());
    }

    @Test void requireAllSaysWhichKindsAreMissing()
    {
        StepHandlers<String> handlers = new StepHandlers<String>().on(EndStep.class, step -> "end");

        IllegalStateException error = assertThrows(IllegalStateException.class, handlers::requireAll);

        assertTrue(error.getMessage().contains("TrekStep"));
        assertTrue(error.getMessage().contains("DialogueStep"));
        assertFalse(error.getMessage().contains("EndStep"));
    }

    @Test void theKindsOfStepAreAllTheOnesOfTheStoryModel()
    {
        // If a new kind of step is added to StoryStep, this list must grow too, and so must the screens of the game
        assertEquals(6, StoryStep.class.getPermittedSubclasses().length);
    }

    @Test void nothingCanBeMissing()
    {
        assertThrows(NullPointerException.class, () -> new StepHandlers<String>().on(null, step -> "x"));
        assertThrows(NullPointerException.class, () -> new StepHandlers<String>().on(EndStep.class, null));
        assertThrows(NullPointerException.class, () -> everything().handle(null));
    }
}
