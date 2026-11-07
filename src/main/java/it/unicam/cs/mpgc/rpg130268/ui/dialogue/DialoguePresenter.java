package it.unicam.cs.mpgc.rpg130268.ui.dialogue;

import it.unicam.cs.mpgc.rpg130268.game.DialogueLine;

import java.util.List;
import java.util.Objects;

/* *
    The logic of a dialogue. It knows which line is on, how much of it is written and what the "next" of the player does
    The window draws view() and tells the presenter that time goes on (tick) and that the player pressed Enter or
    clicked (advance)

    The lines are written letter after letter, and "next" does one of two things. While the line is being written
    it writes the rest at once. When the line is complete the window shows the little triangle (lineComplete)

    This basically works like every other dialogue in a JRPG
* */
public final class DialoguePresenter
{
    public static final double CHARS_PER_SECOND = 45;

    public record View(String speaker, String written, String text, boolean lineComplete, int lineNumber, int lineCount)
    {
        public boolean isNarration() { return speaker.isBlank(); }
    }

    public enum Result { NONE, FINISHED }

    private final List<DialogueLine> lines;
    private int index = 0;
    private double written = 0;         // the letters of the line that are written
    private boolean finished = false;

    public DialoguePresenter(List<DialogueLine> lines)
    {
        Objects.requireNonNull(lines, "lines must not be null");

        if (lines.isEmpty())
            throw new IllegalArgumentException("a dialogue needs at least one line");

        this.lines = List.copyOf(lines);
    }

    public View view()
    {
        DialogueLine line = lines.get(index);
        String text = line.text();
        String shown = text.substring(0, writtenLetters(text));

        return new View(line.speaker(), shown, text, shown.length() == text.length(), index + 1, lines.size());
    }

    // Time goes on and the line is written a bit more. Does nothing if the line is complete or the dialogue is over
    public void tick(long elapsedMs)
    {
        if (finished || elapsedMs <= 0)
            return;

        written += CHARS_PER_SECOND * elapsedMs / 1000.0;
    }

    public Result advance()
    {
        if (finished)
            return Result.NONE;

        String text = lines.get(index).text();

        if (writtenLetters(text) < text.length())
        {
            written = text.length();
            return Result.NONE;
        }

        if (index + 1 < lines.size())
        {
            index++;
            written = 0;
            return Result.NONE;
        }

        finished = true;
        return Result.FINISHED;
    }

    public boolean isFinished() { return finished; }

    private int writtenLetters(String text) { return (int) Math.min(text.length(), Math.floor(written)); }
}
