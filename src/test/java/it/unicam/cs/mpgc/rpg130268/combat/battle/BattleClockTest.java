package it.unicam.cs.mpgc.rpg130268.combat.battle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleClockTest
{
    @Test void aClockStartsAtZero()
    {
        assertEquals(0, new BattleClock().getNowMs());
    }

    @Test void advancingAddsTheTime()
    {
        BattleClock clock = new BattleClock();

        clock.advance(16);
        clock.advance(34);

        assertEquals(50, clock.getNowMs());
    }

    @Test void standingStillIsAllowed()
    {
        BattleClock clock = new BattleClock();

        clock.advance(0);

        assertEquals(0, clock.getNowMs());
    }

    @Test void timeNeverGoesBack()
    {
        assertThrows(IllegalArgumentException.class, () -> new BattleClock().advance(-1));
    }
}