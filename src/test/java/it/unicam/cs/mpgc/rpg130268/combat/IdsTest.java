package it.unicam.cs.mpgc.rpg130268.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdsTest
{
    @Test void aNameBecomesAnIdOfLowercaseWordsJoinedByDashes()
    {
        assertEquals("cuts-for-ink", Ids.slug("Cuts for Ink"));
        assertEquals("tagli-a-croce", Ids.slug("Tagli a croce"));
        assertEquals("a", Ids.slug("A"));
    }

    @Test void accentsAndSymbolsAreRemoved()
    {
        assertEquals("perche-no", Ids.slug("Perché no?"));
        assertEquals("heal-ally", Ids.slug("  Heal   Ally!! "));
    }

    @Test void aNameWithNothingToKeepGivesNoId()
    {
        assertThrows(IllegalArgumentException.class, () -> Ids.slug("!!!"));
        assertThrows(IllegalArgumentException.class, () -> Ids.slug("   "));
        assertThrows(NullPointerException.class, () -> Ids.slug(null));
    }

    @Test void aWellFormedIdIsAccepted()
    {
        assertEquals("club-swing", Ids.require("club-swing"));
        assertEquals("a1", Ids.require("a1"));
        assertEquals("2-hits", Ids.require("2-hits"));
    }

    @Test void aBadlyFormedIdIsRefused()
    {
        for (String bad : new String[] {"", "Club", "club swing", "-club", "club-", "club--swing", "club_swing", "perché"})
            assertThrows(IllegalArgumentException.class, () -> Ids.require(bad), bad);

        assertThrows(NullPointerException.class, () -> Ids.require(null));
    }
}
