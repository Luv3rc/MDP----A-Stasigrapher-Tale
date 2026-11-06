package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameSnapshotTest
{
    private static MemberState member(String id)
    {
        return new MemberState(id, 1, 0, 100, StatPoints.NONE, 0, "thesmosia", List.of("basic-attack"));
    }

    @Test void aSnapshotIsTheStepTheTrekAndTheParty()
    {
        GameSnapshot snapshot = new GameSnapshot("forest", 2, List.of(member("gunther"), member("squire")));

        assertEquals("forest", snapshot.stepId());
        assertEquals(2, snapshot.trekProgress());
        assertEquals(List.of("gunther", "squire"), snapshot.party().stream().map(MemberState::id).toList());
    }

    @Test void thePartyIsCopied()
    {
        ArrayList<MemberState> party = new ArrayList<>(List.of(member("gunther")));
        GameSnapshot snapshot = new GameSnapshot("intro", 0, party);

        party.clear();

        assertEquals(1, snapshot.party().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.party().add(member("squire")));
    }

    @Test void twoSnapshotsOfTheSameMomentAreEqual()
    {
        assertEquals(new GameSnapshot("intro", 0, List.of(member("gunther"))), new GameSnapshot("intro", 0, List.of(member("gunther"))));
        assertNotEquals(new GameSnapshot("intro", 0, List.of(member("gunther"))), new GameSnapshot("intro", 1, List.of(member("gunther"))));
    }

    @Test void badSnapshotsAreRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> new GameSnapshot(" ", 0, List.of(member("gunther"))));
        assertThrows(IllegalArgumentException.class, () -> new GameSnapshot("intro", -1, List.of(member("gunther"))));
        assertThrows(IllegalArgumentException.class, () -> new GameSnapshot("intro", 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new GameSnapshot("intro", 0, List.of(member("gunther"), member("gunther"))));
        assertThrows(NullPointerException.class, () -> new GameSnapshot(null, 0, List.of(member("gunther"))));
    }
}
