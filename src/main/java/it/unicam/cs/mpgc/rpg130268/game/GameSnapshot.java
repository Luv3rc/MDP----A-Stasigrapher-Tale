package it.unicam.cs.mpgc.rpg130268.game;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/* *
    The game at a moment, enough to come back to it. The id of the step the player is in, how far
    he is in the trek (0 if the step is not a trek) and how every member of the party is. It is what
    a save file holds and what GoBack returns to
* */
public record GameSnapshot(String stepId, int trekProgress, List<MemberState> party)
{
    public GameSnapshot
    {
        Objects.requireNonNull(stepId, "stepId must not be null");
        Objects.requireNonNull(party, "party must not be null");

        if (stepId.isBlank())
            throw new IllegalArgumentException("stepId must not be blank");

        if (trekProgress < 0)
            throw new IllegalArgumentException("trekProgress must not be negative");

        if (party.isEmpty())
            throw new IllegalArgumentException("a party needs at least one member");

        party = List.copyOf(party);

        Set<String> seen = new HashSet<>();
        for (MemberState member : party)
            if (!seen.add(member.id()))
                throw new IllegalArgumentException("the member \"" + member.id() + "\" is in the party twice");
    }
}