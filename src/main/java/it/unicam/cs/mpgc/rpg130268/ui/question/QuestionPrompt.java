package it.unicam.cs.mpgc.rpg130268.ui.question;

import java.util.Objects;
import java.util.Optional;

/* *
    The state of the question that can be on top of a screen. A presenter that asks things (the main
    menu, the defeat) has one and gives its view() to the window

    A question always starts with Cancel selected, so a distracted Enter does nothing bad. Moving goes from one
    button to the other. A notice has only one button, which is always the selected one.
    The prompt does not know what the question is about. The presenter that asked remembers it, and decides what each
    answer means
* */
public final class QuestionPrompt
{
    private QuestionView question;                  // null when nobody is asking anything
    private Choice selected = Choice.CANCEL;

    public void ask(String title, String message)
    {
        start(new QuestionView(title, message, Choice.CANCEL));
    }

    // Starts a question with its own words
    public void ask(String title, String message, String confirmLabel, String cancelLabel)
    {
        start(new QuestionView(title, message, confirmLabel, cancelLabel, Choice.CANCEL));
    }

    public void notice(String title, String message)
    {
        start(QuestionView.notice(title, message));
    }

    public boolean isAsking() { return question != null; }

    // Cancel if nobody is asking
    public Choice selected() { return selected; }

    public void toggle()
    {
        if (question == null || !question.hasCancel())
            return;

        selected = selected == Choice.CONFIRM ? Choice.CANCEL : Choice.CONFIRM;
    }

    public void close()
    {
        question = null;
        selected = Choice.CANCEL;
    }

    public Optional<QuestionView> view()
    {
        return Optional.ofNullable(question).map(asked ->
                new QuestionView(asked.title(), asked.message(), asked.confirmLabel(), asked.cancelLabel(), selected));
    }

    private void start(QuestionView asked)
    {
        Objects.requireNonNull(asked, "the question must not be null");

        question = asked;
        selected = asked.hasCancel() ? Choice.CANCEL : Choice.CONFIRM;
    }
}
