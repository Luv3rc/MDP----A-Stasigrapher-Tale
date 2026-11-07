package it.unicam.cs.mpgc.rpg130268.ui.question;

import java.util.Objects;

/* *
    What a question on top of a screen shows ("Return to desktop?" with its two buttons) and which button is selected
    Every screen that asks something (the main menu, the defeat) builds one of these and the QuestionPopup draws it
    The labels of the buttons are Confirm and Cancel unless the question says otherwise ("Yes" and "No", for example)
    A notice is a question with only one button, to tell the player something he has to read ("The saved game can't
    be loaded"). It has no cancel label, so no cancel button
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

    // A message with just an OK button
    public static QuestionView notice(String title, String message)
    {
        return new QuestionView(title, message, "OK", "", Choice.CONFIRM);
    }

    public boolean hasCancel() { return !cancelLabel.isBlank(); }

    public QuestionView(String title, String message, Choice selected)
    {
        this(title, message, "Confirm", "Cancel", selected);
    }
}
