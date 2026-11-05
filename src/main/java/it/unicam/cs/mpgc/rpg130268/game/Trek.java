package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.game.StoryStep.TrekStep;

import java.util.Objects;
import java.util.random.RandomGenerator;

// A walk (see TrekStep), how far the player is and what happens at every step
// The random generator is given from outside
final class Trek
{
    // What a step of the walk brings
    sealed interface Result
    {
        record Quiet() implements Result { }                              // nothing, the walk goes on
        record Ambush(Encounter encounter) implements Result { }          // an enemy
        record Arrived() implements Result { }                            // the walk is over
    }

    private final TrekStep trek;
    private final RandomGenerator random;
    private int progress;

    Trek(TrekStep trek, int progress, RandomGenerator random)
    {
        this.trek = Objects.requireNonNull(trek, "trek must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");

        if (progress < 0 || progress >= trek.length())
            throw new IllegalArgumentException("the progress must be from 0 to " + (trek.length() - 1));

        this.progress = progress;
    }

    // How many steps have been walked
    int progress() { return progress; }

    // One step further. The last one always gets out; the others may be an ambush
    Result walk()
    {
        progress++;

        if (progress >= trek.length())
            return new Result.Arrived();

        if (random.nextDouble() < trek.encounterChance())
            return new Result.Ambush(trek.encounters().get(random.nextInt(trek.encounters().size())));

        return new Result.Quiet();
    }
}
