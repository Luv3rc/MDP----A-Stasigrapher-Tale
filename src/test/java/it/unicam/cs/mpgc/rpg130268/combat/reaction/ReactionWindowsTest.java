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

    @Test void windowIsSplitThreeQuartersBeforeAndOneQuarterAfter()
    {
        ReactionWindows windows = ReactionWindows.defaults();

        assertEquals(120, windows.earlyParryWindowMs());
        assertEquals(40, windows.lateParryWindowMs());
        assertEquals(225, windows.earlyDodgeWindowMs());
        assertEquals(75, windows.lateDodgeWindowMs());
    }

    @Test void earlyAndLatePartsAlwaysAddUpToTheWholeWindow()
    {
        ReactionWindows odd = new ReactionWindows(171, 301);

        assertEquals(171, odd.earlyParryWindowMs() + odd.lateParryWindowMs());
        assertEquals(301, odd.earlyDodgeWindowMs() + odd.lateDodgeWindowMs());
    }

    @Test void theEarlyPartIsAlwaysTheBiggerOne()
    {
        ReactionWindows windows = new ReactionWindows(170, 333);

        assertTrue(windows.earlyParryWindowMs() > windows.lateParryWindowMs());
        assertTrue(windows.earlyDodgeWindowMs() > windows.lateDodgeWindowMs());
    }
}
