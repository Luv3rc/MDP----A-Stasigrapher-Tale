package it.unicam.cs.mpgc.rpg130268.ui.question;

import java.util.Objects;

/* *
    What a question on top of a screen shows ("Return to desktop?" with its two buttons) and which button is selected
* */
public record QuestionView(String title, String message, String confirmLabel, String cancelLabel, Choice selected)
{
    public QuestionView
    {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(confirmLabel, "confirmLabel must not be null");
        Objects.requireNonNull(cancelLabel, "cancelLabel must not be null");
        Objects.requireNonNull(selected, "selected must not be null");
    }

    public QuestionView(String title, String message, Choice selected)
    {
        this(title, message, "Confirm", "Cancel", selected);
    }
}
