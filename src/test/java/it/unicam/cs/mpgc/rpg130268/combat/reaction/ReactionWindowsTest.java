package it.unicam.cs.mpgc.rpg130268.combat.reaction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReactionWindowsTest
{
    @Test void defaultsMatchTheDesign()
    {
        ReactionWindows windows = ReactionWindows.defaults();
        assertEquals(160, windows.parryWindowMs());
        assertEquals(300, windows.dodgeWindowMs());
    }

    @Test void customWindowsKeepTheirValues()
    {
        ReactionWindows windows = new ReactionWindows(250, 400);
        assertEquals(250, windows.parryWindowMs());
        assertEquals(400, windows.dodgeWindowMs());
    }

    @Test void zeroOrNegativeParryWindowThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new ReactionWindows(0, 300));
        assertThrows(IllegalArgumentException.class, () -> new ReactionWindows(-1, 300));
    }

    @Test void dodgeWindowSmallerThanParryWindowThrows()
    {
        assertThrows(IllegalArgumentException.class, () -> new ReactionWindows(300, 160));
    }

    @Test void equalWindowsAreNotAllowed()
    {
        assertThrows(IllegalArgumentException.class, () -> new ReactionWindows(200, 200));
    }
}
