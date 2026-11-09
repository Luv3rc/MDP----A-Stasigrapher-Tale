package it.unicam.cs.mpgc.rpg130268.ui.defeat;

import it.unicam.cs.mpgc.rpg130268.ui.question.Choice;
import it.unicam.cs.mpgc.rpg130268.ui.question.QuestionPrompt;
import it.unicam.cs.mpgc.rpg130268.ui.question.QuestionView;

import java.util.Optional;

/* *
    The logic of the screen after a lost battle. It knows what is on the screen at every moment,
    and the window draws view() and tells the presenter that time goes on (tick) and what the player does

    It happens in three moments, one after the other.
    - The screen gets darker and darker until it is completely black.
    - The red phrase fades in, and it stays. It is alone for a moment, so it can be read.
    - The question "Do you want to retry the battle?" appears over it, with Yes and No

    Nothing the player presses is heard until the question is there, so the whole thing is always seen, and the
    question has to be answered
* */
public final class DefeatPresenter
{
    public enum Action { NONE, RETRY, GO_BACK }

    // What the window shows. The darkness goes from 0 (the picture as it was) to 1 (all black), the opacity of the
    // phrase goes from 0 to 1, and the question is the question, duh
    public record View(String phrase, double darkness, double phraseOpacity, Optional<QuestionView> question) { }

    public static final String PHRASE = "Thesmos won't ever be freed";
    public static final String QUESTION = "Do you want to retry the battle?";

    static final long DARKEN_MS = 3000;
    static final long FADE_IN_MS = 1500;
    static final long READ_MS = 1000;
    static final long QUESTION_AT_MS = DARKEN_MS + FADE_IN_MS + READ_MS;

    private final QuestionPrompt prompt = new QuestionPrompt();
    private long elapsedMs = 0;
    private boolean answered = false;


    // ---------- What to show ----------

    public View view() { return new View(PHRASE, darkness(), phraseOpacity(), prompt.view()); }

    public boolean isQuestionTime() { return elapsedMs >= QUESTION_AT_MS; }


    // ---------- Time ----------

    public void tick(long ms)
    {
        if (ms < 0)
            throw new IllegalArgumentException("time cannot go back");

        elapsedMs += ms;

        if (isQuestionTime() && !prompt.isAsking() && !answered)
            prompt.ask(QUESTION, "", "Yes", "No");
    }


    // ---------- What the player does ----------

    public void move()
    {
        if (!answered)
            prompt.toggle();
    }

    public Action activate() { return choose(prompt.selected()); }

    public Action choose(Choice answer)
    {
        if (!prompt.isAsking() || answered)
            return Action.NONE;

        answered = true;
        prompt.close();

        return answer == Choice.CONFIRM ? Action.RETRY : Action.GO_BACK;
    }


    // ---------- Inside ----------

    private double darkness() { return Math.min(1, (double) elapsedMs / DARKEN_MS); }

    private double phraseOpacity()
    {
        if (elapsedMs <= DARKEN_MS)
            return 0;

        return Math.min(1, (double) (elapsedMs - DARKEN_MS) / FADE_IN_MS);
    }
}
