package it.unicam.cs.mpgc.rpg130268.game;

import java.util.Objects;

public record DialogueLine(String speaker, String text)
{
    public DialogueLine
    {
        Objects.requireNonNull(speaker, "speaker must not be null");
        Objects.requireNonNull(text, "text must not be null");

        if (text.isBlank())
            throw new IllegalArgumentException("text must not be blank");
    }

    public static DialogueLine narration(String text) { return new DialogueLine("", text); }

    public boolean isNarration() { return speaker.isBlank(); }
}
