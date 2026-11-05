package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.game.data.MemberCatalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/* *
    The members, in order (the first one is the one the story is about). It lives as long
    as the game, so health and XP go from a step to the next.
    A PartyMember can't be told "you are level 3 now", so going back to a state means making everybody again
    (from their MemberState, with the MemberCatalog).
    Whoever uses the party must always ask for the members and never keep them
* */
final class Party
{
    private final MemberCatalog catalog;
    private List<PartyMember> members;

    Party(MemberCatalog catalog)
    {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        reset();
    }

    void reset()
    {
        members = catalog.startingParty().stream().map(catalog::newMember).toList();
    }

    void restore(List<MemberState> states)
    {
        Objects.requireNonNull(states, "states must not be null");

        if (states.isEmpty())
            throw new IllegalArgumentException("a party needs at least one member");

        Set<String> seen = new HashSet<>();
        for (MemberState state : states)
            if (!seen.add(state.id()))
                throw new IllegalArgumentException("the member \"" + state.id() + "\" is in the party twice");

        List<PartyMember> rebuilt = new ArrayList<>();
        for (MemberState state : states)
            rebuilt.add(catalog.create(state));

        members = List.copyOf(rebuilt);
    }

    List<MemberState> state() { return members.stream().map(MemberState::of).toList(); }

    List<PartyMember> members() { return members; }

    PartyMember protagonist() { return members.getFirst(); }

    PartyMember member(String id)
    {
        return members.stream().filter(member -> member.getId().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("there is no member \"" + id + "\" in the party"));
    }
}
