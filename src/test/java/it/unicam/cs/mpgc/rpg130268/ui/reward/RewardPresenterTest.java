package it.unicam.cs.mpgc.rpg130268.ui.reward;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilityCatalog;
import it.unicam.cs.mpgc.rpg130268.game.MemberReward;
import it.unicam.cs.mpgc.rpg130268.game.MemberState;
import it.unicam.cs.mpgc.rpg130268.game.Reward;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.MemberCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.WeaponCatalog;
import it.unicam.cs.mpgc.rpg130268.ui.reward.RewardPresenter.MemberView;
import it.unicam.cs.mpgc.rpg130268.ui.reward.RewardPresenter.StatView;
import it.unicam.cs.mpgc.rpg130268.ui.reward.RewardPresenter.View;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RewardPresenterTest
{
    private static final MemberCatalog MEMBERS = MemberCatalog.fromResource(GameData.MEMBERS_FILE,
            WeaponCatalog.fromResource(GameData.WEAPONS_FILE), PartyAbilityCatalog.standard());

    // Gunther at a level, with the points he has put in his stats and the ones he can still spend. He is made from the
    // weapon and the abilities of the file, and not from its starting numbers, so tuning the file does not change these tests
    private static PartyMember gunther(int level, StatPoints points, int unspent)
    {
        MemberState start = MEMBERS.startingState("gunther");

        return MEMBERS.create(new MemberState("gunther", level, 0, 1, points, unspent, start.weaponId(), start.abilityIds()));
    }

    // Gunther after a battle that gave 250 XP, at level 2 with 50 XP of the 300 he needs for the level 3
    private static PartyMember afterTheTutorial()
    {
        PartyMember gunther = gunther(1, StatPoints.NONE, 0);
        gunther.gainXp(250);

        return gunther;
    }

    private static Reward tutorialReward() { return new Reward(List.of(new MemberReward("gunther", 250, 1, 0, 2, 50, 3))); }

    private static MemberView first(PartyMember gunther, Reward reward)
    {
        return new RewardPresenter(List.of(gunther), reward).view().members().getFirst();
    }

    private static StatView stat(MemberView member, String label)
    {
        return member.stats().stream().filter(s -> s.label().equals(label)).findFirst().orElseThrow();
    }

    @Test void theScreenSaysVictoryAndTheXpOfTheBattle()
    {
        View view = new RewardPresenter(List.of(afterTheTutorial()), tutorialReward()).view();

        assertEquals("VICTORY", view.title());
        assertEquals("250", view.totalXp());
    }

    @Test void bigAmountsOfXpHaveSeparators()
    {
        PartyMember gunther = gunther(1, StatPoints.NONE, 0);
        Reward reward = new Reward(List.of(new MemberReward("gunther", 30_392, 1, 0, 1, 0, 0)));

        assertEquals("30,392", new RewardPresenter(List.of(gunther), reward).view().totalXp());
    }

    @Test void aMemberShowsNameLevelXpAndTheLevelUp()
    {
        MemberView member = first(afterTheTutorial(), tutorialReward());

        assertEquals("Gunther", member.name());
        assertEquals("Gunther", member.artName());
        assertEquals(2, member.level());
        assertTrue(member.leveledUp());
        assertEquals(250, member.xpGained());
    }

    @Test void noLevelUpIfTheLevelDidNotChange()
    {
        PartyMember gunther = gunther(1, StatPoints.NONE, 0);
        gunther.gainXp(30);

        MemberView member = first(gunther, new Reward(List.of(new MemberReward("gunther", 30, 1, 0, 1, 30, 0))));

        assertFalse(member.leveledUp());
        assertEquals(0, member.pointsGained());
    }

    @Test void theXpBarIsTheEarnedPartOfTheNextLevel()
    {
        MemberView member = first(afterTheTutorial(), tutorialReward());

        assertEquals(50, member.xp());
        assertEquals(300, member.xpForNext());
        assertEquals(50.0 / 300, member.xpFraction());
    }

    @Test void atTheHighestLevelTheBarIsFull()
    {
        PartyMember gunther = gunther(60, StatPoints.NONE, 0);

        MemberView member = first(gunther, new Reward(List.of(new MemberReward("gunther", 0, 60, 0, 60, 0, 0))));

        assertEquals(0, member.xpForNext());
        assertEquals(1.0, member.xpFraction());
        assertFalse(member.leveledUp());
    }

    @Test void theMemberShowsThePointsHeGotAndTheOnesHeHasToSpend()
    {
        MemberView member = first(afterTheTutorial(), tutorialReward());

        assertEquals(3, member.pointsGained());
        assertEquals(3, member.unspentPoints());                      // he had none and the level gave him these
    }

    @Test void everyStatHasItsValueAndABarThatIsFullAtTheEndOfTheMember()
    {
        PartyMember gunther = gunther(4, new StatPoints(2, 30, 0, 15, 60), 0);
        int cap = gunther.getStatRules().statCap();
        int healthAtTheCap = gunther.getStatRules().baseHealth() + cap * gunther.getStatRules().healthPerPoint();

        MemberView member = first(gunther, new Reward(List.of(new MemberReward("gunther", 0, 4, 0, 4, 0, 0))));

        assertEquals(List.of("Max health", "Strength", "Defense", "Agility", "Luck"),
                member.stats().stream().map(StatView::label).toList());
        assertEquals(gunther.getStats().maxHealth(), stat(member, "Max health").value());
        assertEquals((double) gunther.getStats().maxHealth() / healthAtTheCap, stat(member, "Max health").fraction());   // full at the health that the cap gives
        assertEquals(Math.min(1.0, 30.0 / cap), stat(member, "Strength").fraction());                                  // the part of the cap
        assertEquals(0.0, stat(member, "Defense").fraction());
        assertEquals(Math.min(1.0, 15.0 / cap), stat(member, "Agility").fraction());
        assertEquals(Math.min(1.0, 60.0 / cap), stat(member, "Luck").fraction());
    }

    @Test void aMemberStartsWithEmptyBars()
    {
        MemberView member = first(gunther(1, StatPoints.NONE, 0), new Reward(List.of(new MemberReward("gunther", 0, 1, 0, 1, 0, 0))));

        PartyMember gunther = gunther(1, StatPoints.NONE, 0);
        int healthAtTheCap = gunther.getStatRules().baseHealth() + gunther.getStatRules().statCap() * gunther.getStatRules().healthPerPoint();

        assertEquals(0.0, stat(member, "Strength").fraction());
        assertEquals((double) gunther.getStatRules().baseHealth() / healthAtTheCap, stat(member, "Max health").fraction());
    }

    @Test void thereIsAPanelForEveryMemberInTheRewardInOrder()
    {
        PartyMember gunther = gunther(1, StatPoints.NONE, 0);
        Reward reward = new Reward(List.of(new MemberReward("gunther", 100, 1, 0, 1, 100, 0)));

        View view = new RewardPresenter(List.of(gunther), reward).view();

        assertEquals(1, view.members().size());
        assertEquals("Gunther", view.members().getFirst().name());
    }

    @Test void aRewardNeedsMembersThatAreInTheParty()
    {
        PartyMember gunther = gunther(1, StatPoints.NONE, 0);
        Reward ofSomeoneElse = new Reward(List.of(new MemberReward("squire", 0, 1, 0, 1, 0, 0)));

        assertThrows(IllegalArgumentException.class, () -> new RewardPresenter(List.of(gunther), ofSomeoneElse));
        assertThrows(NullPointerException.class, () -> new RewardPresenter(null, tutorialReward()));
        assertThrows(NullPointerException.class, () -> new RewardPresenter(List.of(gunther), null));
    }
}
