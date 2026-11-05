package it.unicam.cs.mpgc.rpg130268.game;

import java.util.Objects;

// ! Id is what the saves remember, so it must be unique and not change
public record StoryNode(String id, StoryStep step)
{
    public StoryNode
    {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(step, "step must not be null");

        if (id.isBlank())
            throw new IllegalArgumentException("id must not be blank");
    }
}
