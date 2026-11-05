package it.unicam.cs.mpgc.rpg130268.game;

import java.util.List;
import java.util.Objects;

/* *
    One thing the player is doing at a time. A step is what to show or play
    The UI looks at GameFlow.current(), shows it and tells GameFlow what happened (see StepOutcome)

    The steps of the Story are: DialogueStep, BattleStep, TrekStep and EndStep.
    (GameFlow will add the ones it puts after a battle itself, since they're not part of the story)
* */
public sealed interface StoryStep
{
    // A conversation. It happens in an "area" (the name of the background, blank if there is none)
    // and the protagonist talks with the "partner" (the name of the character that is shown in front of him, blank
    // if there is nobody)
    record DialogueStep(String area, String partner, List<DialogueLine> lines) implements StoryStep
    {
        public DialogueStep
        {
            Objects.requireNonNull(area, "area must not be null");
            Objects.requireNonNull(partner, "partner must not be null");
            Objects.requireNonNull(lines, "lines must not be null");

            if (lines.isEmpty())
                throw new IllegalArgumentException("a dialogue needs at least one line");

            lines = List.copyOf(lines);
        }

        // A dialogue with no background and nobody in front of the protagonist
        public DialogueStep(List<DialogueLine> lines) { this("", "", lines); }
    }

    // A fight. The UI makes the enemies with encounter().spawn()
    record BattleStep(Encounter encounter) implements StoryStep
    {
        public BattleStep { Objects.requireNonNull(encounter, "encounter must not be null"); }
    }

    // A walk (a trek) of "length" steps through a place, e.g. a forest, a cave, a road. The "area" is the name of the
    // background to show. At every step (except the one that gets out) there is a chance of meeting one of the
    // encounters, chosen at random (Persona 1 ahh)
    record TrekStep(String area, int length, double encounterChance, List<Encounter> encounters) implements StoryStep
    {
        public TrekStep
        {
            Objects.requireNonNull(area, "area must not be null");
            Objects.requireNonNull(encounters, "encounters must not be null");

            if (area.isBlank())
                throw new IllegalArgumentException("a trek needs an area (the background to show)");

            if (length < 1)
                throw new IllegalArgumentException("length must be at least 1");

            if (encounterChance < 0 || encounterChance > 1)
                throw new IllegalArgumentException("encounterChance must be between 0 and 1");

            if (encounterChance > 0 && encounters.isEmpty())
                throw new IllegalArgumentException("there must be encounters to meet");

            encounters = List.copyOf(encounters);
        }
    }

    // The end of the demo
    record EndStep(String message) implements StoryStep
    {
        public EndStep { Objects.requireNonNull(message, "message must not be null"); }
    }
}
