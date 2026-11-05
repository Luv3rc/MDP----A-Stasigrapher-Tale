package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.Ids;

public record MemberReward(String memberId, int xpGained, int levelBefore, int xpBefore, int levelAfter, int xpAfter,
                           int pointsGained)
{
    public MemberReward
    {
        Ids.require(memberId);

        if (xpGained < 0)
            throw new IllegalArgumentException("xpGained must not be negative");

        if (levelBefore < 1 || levelAfter < levelBefore)
            throw new IllegalArgumentException("the level can only go up, from 1 on");

        if (xpBefore < 0 || xpAfter < 0)
            throw new IllegalArgumentException("xp must not be negative");

        if (pointsGained < 0)
            throw new IllegalArgumentException("pointsGained must not be negative");
    }

    public boolean leveledUp() { return levelAfter > levelBefore; }

    public int levelsGained() { return levelAfter - levelBefore; }
}
