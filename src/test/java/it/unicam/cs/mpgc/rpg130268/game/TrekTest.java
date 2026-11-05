package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.game.StoryStep.TrekStep;
import it.unicam.cs.mpgc.rpg130268.game.Trek.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class TrekTest
{
    private static final RandomGenerator ANY = () -> 0L;
    private static final Encounter A = () -> null;
    private static final Encounter B = () -> null;

    private static TrekStep trek(int length, double chance, Encounter... encounters)
    {
        return new TrekStep("forest", length, chance, List.of(encounters));
    }

    @Test void withNoChanceTheWalkIsQuietUntilTheLastStep()
    {
        Trek trek = new Trek(trek(3, 0.0, A), 0, ANY);

        assertInstanceOf(Result.Quiet.class, trek.walk());
        assertInstanceOf(Result.Quiet.class, trek.walk());
        assertInstanceOf(Result.Arrived.class, trek.walk());
        assertEquals(3, trek.progress());
    }

    @Test void withSureChanceEveryStepButTheLastIsAnAmbush()
    {
        Trek trek = new Trek(trek(3, 1.0, A), 0, ANY);

        assertSame(A, assertInstanceOf(Result.Ambush.class, trek.walk()).encounter());
        assertSame(A, assertInstanceOf(Result.Ambush.class, trek.walk()).encounter());
        assertInstanceOf(Result.Arrived.class, trek.walk());
    }

    @Test void aWalkOfOneStepIsOverAtOnce()
    {
        Trek trek = new Trek(trek(1, 1.0, A), 0, ANY);

        assertInstanceOf(Result.Arrived.class, trek.walk());
    }

    @Test void theEncounterIsChosenAmongTheOnesOfThePlace()
    {
        RandomGenerator last = () -> -1L;      // the biggest numbers: the last encounter, and no chance... except 1.0
        Trek trek = new Trek(trek(2, 1.0, A, B), 0, last);

        assertSame(B, assertInstanceOf(Result.Ambush.class, trek.walk()).encounter());
    }

    @Test void aWalkCanStartFromWhereItWas()
    {
        Trek trek = new Trek(trek(3, 0.0, A), 2, ANY);

        assertEquals(2, trek.progress());
        assertInstanceOf(Result.Arrived.class, trek.walk());
    }

    @Test void theProgressMustBeInsideTheWalk()
    {
        TrekStep step = trek(3, 0.0, A);

        assertThrows(IllegalArgumentException.class, () -> new Trek(step, 3, ANY));
        assertThrows(IllegalArgumentException.class, () -> new Trek(step, -1, ANY));
    }
}
