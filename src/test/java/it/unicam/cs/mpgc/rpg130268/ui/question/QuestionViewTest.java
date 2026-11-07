package it.unicam.cs.mpgc.rpg130268.ui.question;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionViewTest
{
    @Test void aQuestionHasConfirmAndCancelUnlessItSaysOtherwise()
    {
        QuestionView question = new QuestionView("Return to desktop?", "", Choice.CANCEL);

        assertEquals("Confirm", question.confirmLabel());
        assertEquals("Cancel", question.cancelLabel());
        assertEquals(Choice.CANCEL, question.selected());
    }

    @Test void theButtonsCanHaveTheirOwnWords()
    {
        QuestionView question = new QuestionView("Do you want to retry the battle?", "", "Yes", "No", Choice.CONFIRM);

        assertEquals("Yes", question.confirmLabel());
        assertEquals("No", question.cancelLabel());
    }

    @Test void nothingInAQuestionCanBeMissing()
    {
        assertThrows(NullPointerException.class, () -> new QuestionView(null, "", Choice.CANCEL));
        assertThrows(NullPointerException.class, () -> new QuestionView("Title", null, Choice.CANCEL));
        assertThrows(NullPointerException.class, () -> new QuestionView("Title", "", null));
        assertThrows(NullPointerException.class, () -> new QuestionView("Title", "", null, "No", Choice.CANCEL));
    }
}
