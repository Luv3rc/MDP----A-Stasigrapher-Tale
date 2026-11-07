package it.unicam.cs.mpgc.rpg130268.ui.dialogue;

import it.unicam.cs.mpgc.rpg130268.game.DialogueLine;
import it.unicam.cs.mpgc.rpg130268.ui.dialogue.DialoguePresenter.Result;
import it.unicam.cs.mpgc.rpg130268.ui.dialogue.DialoguePresenter.View;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DialoguePresenterTest
{
    private static final DialogueLine FIRST = new DialogueLine("Gunther", "Hello there.");      // 12 letters
    private static final DialogueLine SECOND = DialogueLine.narration("It rains.");

    private static DialoguePresenter two() { return new DialoguePresenter(List.of(FIRST, SECOND)); }

    // Enough time for the whole line
    private static void writeAll(DialoguePresenter dialogue) { dialogue.tick(60_000); }

    // ---------- Writing ----------

    @Test void aDialogueStartsWithNothingWrittenOfTheFirstLine()
    {
        View view = two().view();

        assertEquals("Gunther", view.speaker());
        assertEquals("", view.written());
        assertEquals("Hello there.", view.text());
        assertFalse(view.lineComplete());
        assertEquals(1, view.lineNumber());
        assertEquals(2, view.lineCount());
    }

    @Test void timeWritesTheLineLetterByLetter()
    {
        DialoguePresenter dialogue = two();

        dialogue.tick(100);                                  // 45 letters a second: 4.5 letters
        assertEquals("Hell", dialogue.view().written());

        dialogue.tick(100);                                  // 9 letters
        assertEquals("Hello th", dialogue.view().written().substring(0, 8));
        assertEquals(9, dialogue.view().written().length());
    }

    @Test void theLineIsCompleteWhenAllTheLettersAreWritten()
    {
        DialoguePresenter dialogue = two();

        writeAll(dialogue);

        assertEquals("Hello there.", dialogue.view().written());
        assertTrue(dialogue.view().lineComplete());
    }

    @Test void timeNeverWritesMoreThanTheLine()
    {
        DialoguePresenter dialogue = two();
        dialogue.tick(1_000_000);

        assertEquals("Hello there.", dialogue.view().written());
    }

    @Test void noTimeOrBackwardsTimeWritesNothing()
    {
        DialoguePresenter dialogue = two();
        dialogue.tick(0);
        dialogue.tick(-500);

        assertEquals("", dialogue.view().written());
    }

    // ---------- Next ----------

    @Test void nextWhileWritingWritesTheRestAtOnceAndDoesNotChangeTheLine()
    {
        DialoguePresenter dialogue = two();
        dialogue.tick(100);

        assertEquals(Result.NONE, dialogue.advance());

        assertTrue(dialogue.view().lineComplete());
        assertEquals(1, dialogue.view().lineNumber());
    }

    @Test void nextWhenTheLineIsCompleteGoesToTheNextLineFromTheStart()
    {
        DialoguePresenter dialogue = two();
        writeAll(dialogue);

        assertEquals(Result.NONE, dialogue.advance());

        View view = dialogue.view();
        assertEquals(2, view.lineNumber());
        assertEquals("", view.written());
        assertTrue(view.isNarration());
        assertFalse(view.lineComplete());
    }

    @Test void aDoubleClickDoesNotSkipALineThatWasNeverRead()
    {
        DialoguePresenter dialogue = two();

        dialogue.advance();                                  // writes the line
        dialogue.advance();                                  // and goes on: but only now that it can be read in full

        assertEquals(2, dialogue.view().lineNumber());
        assertEquals("", dialogue.view().written());
    }

    @Test void theLastLineEndsTheDialogueOnlyOnceItIsComplete()
    {
        DialoguePresenter dialogue = new DialoguePresenter(List.of(SECOND));

        assertEquals(Result.NONE, dialogue.advance());       // writes it
        assertFalse(dialogue.isFinished());

        assertEquals(Result.FINISHED, dialogue.advance());
        assertTrue(dialogue.isFinished());
    }

    @Test void afterTheEndNothingHappensAnymore()
    {
        DialoguePresenter dialogue = new DialoguePresenter(List.of(SECOND));
        dialogue.advance();
        dialogue.advance();

        assertEquals(Result.NONE, dialogue.advance());
        dialogue.tick(1000);
        assertTrue(dialogue.isFinished());
    }

    @Test void thereIsNoFinishedUntilEveryLineIsRead()
    {
        DialoguePresenter dialogue = two();

        for (int click = 0; click < 3; click++)
            assertEquals(Result.NONE, dialogue.advance());

        assertEquals(Result.FINISHED, dialogue.advance());
    }

    // ---------- Wrong use ----------

    @Test void aDialogueNeedsLines()
    {
        assertThrows(IllegalArgumentException.class, () -> new DialoguePresenter(List.of()));
        assertThrows(NullPointerException.class, () -> new DialoguePresenter(null));
    }

    @Test void theLinesAreCopied()
    {
        java.util.ArrayList<DialogueLine> lines = new java.util.ArrayList<>(List.of(FIRST));
        DialoguePresenter dialogue = new DialoguePresenter(lines);

        lines.add(SECOND);
        dialogue.advance();

        assertEquals(Result.FINISHED, dialogue.advance());
    }
}
