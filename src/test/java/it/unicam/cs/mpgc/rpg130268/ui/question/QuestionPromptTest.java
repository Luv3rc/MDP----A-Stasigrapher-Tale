package it.unicam.cs.mpgc.rpg130268.ui.question;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionPromptTest
{
    @Test void nobodyAsksAnythingAtTheStart()
    {
        QuestionPrompt prompt = new QuestionPrompt();

        assertFalse(prompt.isAsking());
        assertTrue(prompt.view().isEmpty());
        assertEquals(Choice.CANCEL, prompt.selected());
    }

    @Test void aQuestionStartsWithCancelSelected()
    {
        QuestionPrompt prompt = new QuestionPrompt();

        prompt.ask("Return to desktop?", "");

        QuestionView view = prompt.view().orElseThrow();
        assertEquals("Return to desktop?", view.title());
        assertEquals(Choice.CANCEL, view.selected());
        assertEquals("Confirm", view.confirmLabel());
        assertEquals("Cancel", view.cancelLabel());
        assertTrue(view.hasCancel());
    }

    @Test void theWordsOfTheButtonsCanBeChosen()
    {
        QuestionPrompt prompt = new QuestionPrompt();

        prompt.ask("Do you want to retry the battle?", "", "Yes", "No");

        assertEquals("Yes", prompt.view().orElseThrow().confirmLabel());
        assertEquals("No", prompt.view().orElseThrow().cancelLabel());
    }

    @Test void movingGoesFromOneButtonToTheOther()
    {
        QuestionPrompt prompt = new QuestionPrompt();
        prompt.ask("Question", "");

        prompt.toggle();
        assertEquals(Choice.CONFIRM, prompt.selected());
        assertEquals(Choice.CONFIRM, prompt.view().orElseThrow().selected());

        prompt.toggle();
        assertEquals(Choice.CANCEL, prompt.selected());
    }

    @Test void aNewQuestionAlwaysStartsOnCancel()
    {
        QuestionPrompt prompt = new QuestionPrompt();
        prompt.ask("Question", "");
        prompt.toggle();

        prompt.close();
        prompt.ask("Another question", "");

        assertEquals(Choice.CANCEL, prompt.selected());
    }

    @Test void closingTheQuestionLeavesNothingBehind()
    {
        QuestionPrompt prompt = new QuestionPrompt();
        prompt.ask("Question", "");
        prompt.toggle();

        prompt.close();

        assertFalse(prompt.isAsking());
        assertTrue(prompt.view().isEmpty());
        assertEquals(Choice.CANCEL, prompt.selected());
    }

    @Test void aNoticeHasOnlyOkAndItIsAlwaysSelected()
    {
        QuestionPrompt prompt = new QuestionPrompt();

        prompt.notice("Problem", "Something went wrong.");
        prompt.toggle();

        QuestionView view = prompt.view().orElseThrow();
        assertEquals("OK", view.confirmLabel());
        assertFalse(view.hasCancel());
        assertEquals(Choice.CONFIRM, view.selected());
    }

    @Test void movingWhenNobodyAsksDoesNothing()
    {
        QuestionPrompt prompt = new QuestionPrompt();

        prompt.toggle();

        assertEquals(Choice.CANCEL, prompt.selected());
    }
}
