package it.unicam.cs.mpgc.rpg130268.game;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Reward(List<MemberReward> members)
{
    public Reward
    {
        Objects.requireNonNull(members, "members must not be null");

        if (members.isEmpty())
            throw new IllegalArgumentException("a reward is for at least one member");

        members = List.copyOf(members);

        Set<String> seen = new HashSet<>();
        for (MemberReward reward : members)
            if (!seen.add(reward.memberId()))
                throw new IllegalArgumentException("the member \"" + reward.memberId() + "\" is in the reward twice");
    }

    public MemberReward of(String memberId)
    {
        return members.stream().filter(reward -> reward.memberId().equals(memberId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("there is no reward for \"" + memberId + "\""));
    }

    public int totalXp() { return members.stream().mapToInt(MemberReward::xpGained).sum(); }
}
