package it.unicam.cs.mpgc.rpg130268.game;

import java.util.List;
import java.util.Map;
import java.util.Objects;

// A battle that has begun and is not closed yet
// which fight it is, where it was met (the story or a trek) and how the party was before it,
// which is where a retry goes back to
record PendingBattle(Encounter encounter, boolean fromTrek, List<MemberState> partyBefore)
{
    PendingBattle
    {
        Objects.requireNonNull(encounter, "encounter must not be null");
        Objects.requireNonNull(partyBefore, "partyBefore must not be null");

        partyBefore = List.copyOf(partyBefore);
    }

    Reward rewardFor(List<MemberState> partyAfter, Map<String, Integer> xpByMember)
    {
        Objects.requireNonNull(partyAfter, "partyAfter must not be null");
        Objects.requireNonNull(xpByMember, "xpByMember must not be null");

        return new Reward(partyAfter.stream().map(after ->
        {
            MemberState before = partyBefore.stream().filter(state -> state.id().equals(after.id())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("\"" + after.id() + "\" was not in the party before the battle"));

            return new MemberReward(after.id(), xpByMember.getOrDefault(after.id(), 0), before.level(), before.xp(),
                    after.level(), after.xp(), Math.max(0, after.unspentPoints() - before.unspentPoints()));
        }).toList());
    }
}
