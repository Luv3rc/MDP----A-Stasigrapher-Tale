package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XpRulesTest
{
    private static Gunther standing() { return new Gunther(new Stats(100, 10, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults()); }

    private static Gunther fallen()
    {
        Gunther member = standing();
        member.setHp(0);
        return member;
    }

    private static int total(Map<PartyMember, Integer> shares) { return shares.values().stream().mapToInt(Integer::intValue).sum(); }

    @Test void aPartyOfOneGetsEverything()
    {
        Gunther only = standing();

        assertEquals(Map.of(only, 80), XpRules.defaults().split(80, List.of(only)));
    }

    @Test void everybodyStandingGetsTheSameShare()
    {
        Gunther first = standing();
        Gunther second = standing();

        assertEquals(Map.of(first, 50, second, 50), XpRules.defaults().split(100, List.of(first, second)));
    }

    @Test void theFallenGetLessAndWhatTheyLoseGoesToTheOnesStanding()
    {
        Gunther alive = standing();
        Gunther down = fallen();

        Map<PartyMember, Integer> shares = XpRules.defaults().split(100, List.of(alive, down));

        assertEquals(60, shares.get(alive));       // his 50 and the 10 that the other lost (20%)
        assertEquals(40, shares.get(down));
    }

    @Test void theCutIsSharedBetweenTheOnesStandingAndNothingIsLost()
    {
        Gunther first = standing();
        Gunther down = fallen();
        Gunther second = standing();

        Map<PartyMember, Integer> shares = XpRules.defaults().split(100, List.of(first, down, second));

        assertEquals(26, shares.get(down));        // 33 of its part, less 20%
        assertEquals(37, shares.get(first));       // 33 + half of (7 lost + 1 of the remainder)
        assertEquals(37, shares.get(second));
        assertEquals(100, total(shares));
    }

    @Test void theRemainderGoesToTheOnesStandingInOrder()
    {
        Gunther first = standing();
        Gunther second = standing();
        Gunther third = standing();

        Map<PartyMember, Integer> shares = XpRules.defaults().split(100, List.of(first, second, third));

        assertEquals(34, shares.get(first));
        assertEquals(33, shares.get(second));
        assertEquals(33, shares.get(third));
    }

    @Test void thePenaltyIsARuleOfTheGame()
    {
        Gunther alive = standing();
        Gunther down = fallen();

        assertEquals(Map.of(alive, 50, down, 50), new XpRules(0.0).split(100, List.of(alive, down)));
        assertEquals(Map.of(alive, 100, down, 0), new XpRules(1.0).split(100, List.of(alive, down)));
        assertEquals(Map.of(alive, 55, down, 45), new XpRules(0.10).split(100, List.of(alive, down)));
    }

    @Test void withNobodyStandingOrNothingToShareNobodyGetsAnything()
    {
        Gunther down = fallen();
        Gunther alive = standing();

        assertEquals(Map.of(down, 0), XpRules.defaults().split(100, List.of(down)));
        assertEquals(Map.of(alive, 0), XpRules.defaults().split(0, List.of(alive)));
    }

    @Test void badValuesAreRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> new XpRules(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new XpRules(1.1));
        assertThrows(IllegalArgumentException.class, () -> XpRules.defaults().split(-1, List.of(standing())));
        assertThrows(NullPointerException.class, () -> XpRules.defaults().split(10, null));
    }
}
