package it.unicam.cs.mpgc.rpg130268.game;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RewardTest
{
    private static MemberState state(String id, int level, int xp, int unspent)
    {
        return new MemberState(id, level, xp, 100, it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints.NONE, unspent,
                "thesmosia", List.of("basic-attack"));
    }

    @Test void aRewardIsMadeOfWhatWasBeforeAndAfterOfEveryMember()
    {
        PendingBattle battle = new PendingBattle(() -> null, false, List.of(state("gunther", 1, 10, 0), state("squire", 2, 0, 3)));

        Reward reward = battle.rewardFor(List.of(state("gunther", 2, 30, 3), state("squire", 2, 12, 3)),
                Map.of("gunther", 220, "squire", 12));

        assertEquals(new MemberReward("gunther", 220, 1, 10, 2, 30, 3), reward.of("gunther"));
        assertEquals(new MemberReward("squire", 12, 2, 0, 2, 12, 0), reward.of("squire"));
        assertEquals(232, reward.totalXp());
        assertEquals(List.of("gunther", "squire"), reward.members().stream().map(MemberReward::memberId).toList());
    }

    @Test void aMemberWhoGotNothingHasAnEmptyReward()
    {
        PendingBattle battle = new PendingBattle(() -> null, true, List.of(state("gunther", 1, 10, 0)));

        MemberReward reward = battle.rewardFor(List.of(state("gunther", 1, 10, 0)), Map.of()).of("gunther");

        assertEquals(0, reward.xpGained());
        assertFalse(reward.leveledUp());
        assertEquals(0, reward.pointsGained());
    }

    @Test void theLevelsAndThePointsAreTheOnesGained()
    {
        MemberReward reward = new MemberReward("gunther", 500, 1, 0, 3, 0, 6);

        assertTrue(reward.leveledUp());
        assertEquals(2, reward.levelsGained());
    }

    @Test void aMemberWhoWasNotInThePartyBeforeIsAnError()
    {
        PendingBattle battle = new PendingBattle(() -> null, false, List.of(state("gunther", 1, 0, 0)));

        assertThrows(IllegalArgumentException.class,
                () -> battle.rewardFor(List.of(state("gunther", 1, 0, 0), state("squire", 1, 0, 0)), Map.of()));
    }

    @Test void thePartyBeforeIsKeptAsItWas()
    {
        java.util.ArrayList<MemberState> before = new java.util.ArrayList<>(List.of(state("gunther", 1, 0, 0)));
        PendingBattle battle = new PendingBattle(() -> null, true, before);

        before.clear();

        assertEquals(1, battle.partyBefore().size());
        assertTrue(battle.fromTrek());
    }

    @Test void badRewardsAreRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> new MemberReward("gunther", -1, 1, 0, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MemberReward("gunther", 0, 2, 0, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MemberReward("gunther", 0, 1, -1, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MemberReward("Gunther", 0, 1, 0, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Reward(List.of()));
        MemberReward one = new MemberReward("gunther", 0, 1, 0, 1, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new Reward(List.of(one, one)));
    }

    @Test void whatTheBattleGaveIsAMapByIdAndCannotBeNegative()
    {
        assertEquals(Map.of("gunther", 5), new StepOutcome.BattleWon(Map.of("gunther", 5)).xpByMember());
        assertThrows(IllegalArgumentException.class, () -> new StepOutcome.BattleWon(Map.of("gunther", -1)));
    }
}
