package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.FirstUsableAbility;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilities;
import it.unicam.cs.mpgc.rpg130268.game.StepOutcome.*;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.*;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.MemberCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.TestMembers;
import it.unicam.cs.mpgc.rpg130268.game.data.WeaponCatalog;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// The game with a party of two: nothing in the flow is about one member
class GameFlowPartyTest
{
    private static final MemberCatalog TWO = MemberCatalog.load(
            new ByteArrayInputStream(TestMembers.TWO_MEMBERS.getBytes(StandardCharsets.UTF_8)), "test.xml",
            WeaponCatalog.fromResource(GameData.WEAPONS_FILE), PartyAbilities.standard(), TestMembers.withSquire());

    private static final Encounter DUMMY = () -> new EncounterSetup(
            List.of(new Enemy("Dummy", new Stats(10, 1, 1, 1, 0), 1, 100)), new FirstUsableAbility(), "arena");

    private static final Story STORY = new Story(List.of(
            new StoryNode("tutorial", new BattleStep(DUMMY)),
            new StoryNode("end", new EndStep("the end"))));

    private static GameFlow flow() { return new GameFlow(STORY, TWO, () -> 0L); }

    @Test void theGameStartsWithTheWholeStartingParty()
    {
        GameFlow flow = flow();

        assertEquals(List.of("gunther", "squire"), flow.getParty().stream().map(m -> m.getId()).toList());
        assertEquals("Gunther", flow.getProtagonist().getName());
    }

    @Test void theRewardHasOneLinePerMemberInTheOrderOfTheParty()
    {
        GameFlow flow = flow();

        flow.getParty().get(0).gainXp(250);
        flow.getParty().get(1).gainXp(40);
        flow.complete(new BattleWon(Map.of("gunther", 250, "squire", 40)));

        Reward reward = assertInstanceOf(RewardStep.class, flow.current()).reward();

        assertEquals(List.of("gunther", "squire"), reward.members().stream().map(MemberReward::memberId).toList());
        assertTrue(reward.of("gunther").leveledUp());
        assertFalse(reward.of("squire").leveledUp());
        assertEquals(290, reward.totalXp());
    }

    @Test void aRetryBringsBackEveryMemberAsHeWas()
    {
        GameFlow flow = flow();
        int gunther = flow.getParty().get(0).getHp();
        int squire = flow.getParty().get(1).getHp();

        flow.getParty().get(0).takeDamage(70);
        flow.getParty().get(1).gainXp(500);
        flow.complete(new BattleLost());
        flow.complete(new Retry());

        assertEquals(gunther, flow.getParty().get(0).getHp());
        assertEquals(squire, flow.getParty().get(1).getHp());
        assertEquals(1, flow.getParty().get(1).getLevel());
    }

    @Test void aSaveHoldsEveryMemberAndRestoresThem()
    {
        GameFlow flow = new GameFlow(new Story(List.of(
                new StoryNode("talk", new DialogueStep(List.of(DialogueLine.narration("..."))))
                , new StoryNode("end", new EndStep("the end")))), TWO, () -> 0L);

        flow.getParty().get(1).gainXp(300);
        GameSnapshot saved = flow.save().orElseThrow();
        flow.newGame();
        flow.restore(saved);

        assertEquals(List.of("gunther", "squire"), saved.party().stream().map(MemberState::id).toList());
        assertEquals(2, flow.getParty().get(1).getLevel());
        assertEquals(1, flow.getParty().get(0).getLevel());
    }
}
