package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.FirstUsableAbility;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilityCatalog;
import it.unicam.cs.mpgc.rpg130268.game.StepOutcome.*;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.*;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.MemberCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.TestMembers;
import it.unicam.cs.mpgc.rpg130268.game.data.WeaponCatalog;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class GameFlowTest
{
    private static final RandomGenerator ANY = () -> 0L;

    private static final Encounter DUMMY = () -> new EncounterSetup(
            List.of(new Enemy("Dummy", new Stats(10, 1, 1, 1, 0), 1, 100)), new FirstUsableAbility(), "arena");

    private static final WeaponCatalog WEAPONS = WeaponCatalog.fromResource(GameData.WEAPONS_FILE);
    private static final MemberCatalog MEMBERS = MemberCatalog.fromResource(GameData.MEMBERS_FILE, WEAPONS, PartyAbilityCatalog.standard());

    private static final DialogueStep TALK = new DialogueStep(List.of(DialogueLine.narration("...")));

    // intro (talk), tutorial (battle), after (talk), forest (3 steps), end
    private static Story story(double encounterChance)
    {
        return new Story(List.of(
                new StoryNode("intro", TALK),
                new StoryNode("tutorial", new BattleStep(DUMMY)),
                new StoryNode("after", TALK),
                new StoryNode("forest", new TrekStep("forest", 3, encounterChance, List.of(DUMMY))),
                new StoryNode("end", new EndStep("the end"))));
    }

    private static GameFlow newFlow(double encounterChance)
    {
        return new GameFlow(story(encounterChance), MEMBERS, ANY);
    }

    private static void finish(GameFlow flow) { flow.complete(new Finished()); }

    // The flow up to the first battle
    private static GameFlow atTheFirstBattle()
    {
        GameFlow flow = newFlow(0);
        finish(flow);
        return flow;
    }

    // The flow up to the forest
    private static GameFlow atTheForest(double encounterChance)
    {
        GameFlow flow = newFlow(encounterChance);
        finish(flow);                                  // talk
        flow.complete(new BattleWon(Map.of()));               // battle
        finish(flow);                                  // reward
        finish(flow);                                  // talk
        return flow;
    }

    // ---------- The story from the start ----------

    @Test void aNewGameStartsWithTheFirstDialogueAndTheStartingParty()
    {
        GameFlow flow = newFlow(0);

        assertSame(TALK, flow.current());
        assertEquals(1, flow.getProtagonist().getLevel());
        assertEquals(0, flow.getProtagonist().getCurrentXp());
        assertEquals(List.of(flow.getProtagonist()), flow.getParty());
        assertEquals("Gunther", flow.getProtagonist().getName());
        assertFalse(flow.isOver());
    }

    @Test void afterTheDialogueTheBattleStarts()
    {
        GameFlow flow = atTheFirstBattle();

        assertEquals(new BattleStep(DUMMY), flow.current());
    }

    @Test void winningShowsTheRewardWithXpAndLevelBeforeAndAfter()
    {
        GameFlow flow = atTheFirstBattle();

        flow.getProtagonist().gainXp(250);                       // what the battle does
        flow.complete(new BattleWon(Map.of("gunther", 250)));

        MemberReward reward = assertInstanceOf(RewardStep.class, flow.current()).reward().of("gunther");
        assertEquals(250, reward.xpGained());
        assertEquals(1, reward.levelBefore());
        assertEquals(0, reward.xpBefore());
        assertEquals(2, reward.levelAfter());
        assertEquals(50, reward.xpAfter());
        assertTrue(reward.leveledUp());
        assertEquals(3, reward.pointsGained());
    }

    @Test void afterTheRewardTheStoryGoesOn()
    {
        GameFlow flow = atTheFirstBattle();
        flow.complete(new BattleWon(Map.of()));
        finish(flow);

        assertSame(TALK, flow.current());
    }

    @Test void gunthersHealthAndXpCarryOverBetweenSteps()
    {
        GameFlow flow = atTheFirstBattle();
        PartyMember gunther = flow.getProtagonist();

        gunther.takeDamage(50);
        flow.complete(new BattleWon(Map.of()));
        finish(flow);

        assertSame(gunther, flow.getProtagonist());
        assertEquals(gunther.getStats().maxHealth() - 50, gunther.getHp());
    }

    // ---------- Losing ----------

    @Test void losingOffersTheChoiceOfTryingAgain()
    {
        GameFlow flow = atTheFirstBattle();
        flow.complete(new BattleLost());

        assertInstanceOf(DefeatStep.class, flow.current());
    }

    @Test void tryingAgainPlaysTheSameBattleWithTheGuntherOfBeforeIt()
    {
        GameFlow flow = atTheFirstBattle();
        int fullHp = flow.getProtagonist().getStats().maxHealth();

        flow.getProtagonist().takeDamage(fullHp);                // the battle goes badly...
        flow.getProtagonist().gainXp(40);
        flow.complete(new BattleLost());
        flow.complete(new Retry());

        assertEquals(new BattleStep(DUMMY), flow.current());
        assertEquals(fullHp, flow.getProtagonist().getHp());
        assertEquals(0, flow.getProtagonist().getCurrentXp());
    }

    @Test void youCanLoseAndTryAgainManyTimes()
    {
        GameFlow flow = atTheFirstBattle();

        for (int i = 0; i < 3; i++)
        {
            flow.complete(new BattleLost());
            flow.complete(new Retry());
            assertEquals(new BattleStep(DUMMY), flow.current());
        }

        flow.complete(new BattleWon(Map.of()));
        assertInstanceOf(RewardStep.class, flow.current());
    }

    @Test void tryingAgainAfterALossInTheForestGoesBackToThatForest()
    {
        GameFlow flow = atTheForest(1.0);
        flow.complete(new Walked());                         // a battle
        flow.complete(new BattleLost());
        flow.complete(new Retry());
        flow.complete(new BattleWon(Map.of()));
        finish(flow);                                        // reward

        assertInstanceOf(TrekStep.class, flow.current());
        assertEquals(1, flow.getTrekProgress());
    }

    // ---------- The forest ----------

    @Test void withNoChanceOfMeetingAnyoneTheForestIsJustThreeStepsAndTheEnd()
    {
        GameFlow flow = atTheForest(0.0);

        flow.complete(new Walked());
        assertInstanceOf(TrekStep.class, flow.current());
        assertEquals(1, flow.getTrekProgress());

        flow.complete(new Walked());
        assertInstanceOf(TrekStep.class, flow.current());

        flow.complete(new Walked());
        assertInstanceOf(EndStep.class, flow.current());
        assertTrue(flow.isOver());
    }

    @Test void withSureEncountersEveryStepButTheLastOneIsABattleAndTheForestContinuesAfterIt()
    {
        GameFlow flow = atTheForest(1.0);

        for (int walked = 1; walked <= 2; walked++)
        {
            flow.complete(new Walked());
            assertEquals(new BattleStep(DUMMY), flow.current());

            flow.complete(new BattleWon(Map.of("gunther", 30)));
            finish(flow);                                    // reward
            assertInstanceOf(TrekStep.class, flow.current());
            assertEquals(walked, flow.getTrekProgress());
        }

        flow.complete(new Walked());                         // the last step gets out, no battle
        assertTrue(flow.isOver());
    }

    @Test void aNewForestStartsFromZero()
    {
        GameFlow flow = atTheForest(0.0);

        assertEquals(0, flow.getTrekProgress());
    }

    // ---------- The end ----------

    @Test void nothingCanBeDoneAfterTheEnd()
    {
        GameFlow flow = atTheForest(0.0);
        flow.complete(new Walked());
        flow.complete(new Walked());
        flow.complete(new Walked());

        assertThrows(IllegalStateException.class, () -> finish(flow));
    }

    // ---------- Wrong outcomes ----------

    @Test void anOutcomeThatDoesNotFitTheStepIsRejectedAndNothingChanges()
    {
        GameFlow flow = newFlow(0);

        assertThrows(IllegalArgumentException.class, () -> flow.complete(new Walked()));
        assertThrows(IllegalArgumentException.class, () -> flow.complete(new BattleWon(Map.of())));
        assertSame(TALK, flow.current());
    }

    @Test void aBattleEndsOnlyWithAVictoryOrADefeat()
    {
        GameFlow flow = atTheFirstBattle();

        assertThrows(IllegalArgumentException.class, () -> finish(flow));
        assertEquals(new BattleStep(DUMMY), flow.current());
    }

    @Test void theDefeatCanOnlyBeAnsweredWithRetryOrGoBack()
    {
        GameFlow flow = atTheFirstBattle();
        flow.complete(new BattleLost());

        assertThrows(IllegalArgumentException.class, () -> finish(flow));
        assertInstanceOf(DefeatStep.class, flow.current());
    }

    // ---------- Saving and loading ----------

    @Test void youCanSaveWhileTalkingOrInTheForestButNotDuringOrRightAfterABattle()
    {
        GameFlow flow = newFlow(1.0);
        assertTrue(flow.save().isPresent());                 // talking

        finish(flow);
        assertTrue(flow.save().isEmpty());                   // battle

        flow.complete(new BattleWon(Map.of()));
        assertTrue(flow.save().isEmpty());                   // reward

        finish(flow);
        finish(flow);
        assertTrue(flow.save().isPresent());                 // forest

        flow.complete(new Walked());
        assertTrue(flow.save().isEmpty());                   // a battle in the forest

        flow.complete(new BattleLost());
        assertTrue(flow.save().isEmpty());                   // defeat
    }

    @Test void aSaveRemembersTheStepByItsId()
    {
        GameFlow flow = atTheForest(0.0);
        flow.complete(new Walked());

        GameSnapshot saved = flow.save().orElseThrow();

        assertEquals("forest", saved.stepId());
        assertEquals(1, saved.trekProgress());
    }

    @Test void restoringAGameBringsBackTheStepTheTrekProgressAndGunther()
    {
        GameFlow flow = atTheForest(0.0);
        flow.getProtagonist().gainXp(230);
        flow.getProtagonist().takeDamage(40);
        flow.complete(new Walked());

        GameSnapshot saved = flow.save().orElseThrow();
        int hp = flow.getProtagonist().getHp();

        flow.complete(new Walked());                         // time goes on...
        flow.complete(new Walked());
        assertTrue(flow.isOver());

        flow.restore(saved);

        assertInstanceOf(TrekStep.class, flow.current());
        assertEquals(1, flow.getTrekProgress());
        assertEquals(2, flow.getProtagonist().getLevel());
        assertEquals(30, flow.getProtagonist().getCurrentXp());
        assertEquals(hp, flow.getProtagonist().getHp());
    }

    @Test void aSaveStillWorksIfTheStoryChangedAndTheStepsMoved()
    {
        GameFlow flow = atTheForest(0.0);
        flow.complete(new Walked());
        GameSnapshot saved = flow.save().orElseThrow();

        // the same story with a new step at the beginning: "forest" is not in the same position anymore
        Story longer = new Story(List.of(
                new StoryNode("prologue", TALK),
                new StoryNode("intro", TALK),
                new StoryNode("tutorial", new BattleStep(DUMMY)),
                new StoryNode("after", TALK),
                new StoryNode("forest", new TrekStep("forest", 3, 0.0, List.of(DUMMY))),
                new StoryNode("end", new EndStep("the end"))));
        GameFlow loaded = new GameFlow(longer, MEMBERS, ANY);

        loaded.restore(saved);

        assertInstanceOf(TrekStep.class, loaded.current());
        assertEquals(1, loaded.getTrekProgress());
    }

    @Test void loadingAfterADefeatLeavesTheDefeatBehind()
    {
        GameFlow flow = newFlow(0);
        GameSnapshot saved = flow.save().orElseThrow();      // at the start

        finish(flow);
        flow.complete(new BattleLost());
        flow.restore(saved);

        assertSame(TALK, flow.current());
    }

    @Test void aSaveForAStepThatDoesNotExistIsRejectedAndNothingChanges()
    {
        GameFlow flow = newFlow(0);

        assertThrows(IllegalArgumentException.class,
                () -> flow.restore(new GameSnapshot("nowhere", 0, flow.save().orElseThrow().party())));
        assertSame(TALK, flow.current());
    }

    @Test void aSaveBeyondTheEndOfTheForestIsRejected()
    {
        GameFlow flow = newFlow(0);

        assertThrows(IllegalArgumentException.class,
                () -> flow.restore(new GameSnapshot("forest", 3, flow.save().orElseThrow().party())));
        assertSame(TALK, flow.current());
    }

    @Test void aNewGameStartsAgainFromTheBeginning()
    {
        GameFlow flow = atTheForest(0.0);
        flow.getProtagonist().gainXp(300);

        flow.newGame();

        assertSame(TALK, flow.current());
        assertEquals(1, flow.getProtagonist().getLevel());
        assertEquals(0, flow.getTrekProgress());
    }

    // ---------- Going back ----------

    @Test void goingBackAfterADefeatInTheTutorialReturnsToTheStartWithGuntherAsHeWas()
    {
        GameFlow flow = atTheFirstBattle();
        int fullHp = flow.getProtagonist().getStats().maxHealth();

        flow.getProtagonist().takeDamage(fullHp);
        flow.getProtagonist().gainXp(40);
        flow.complete(new BattleLost());
        flow.complete(new GoBack());

        assertSame(TALK, flow.current());                    // the first dialogue
        assertEquals(fullHp, flow.getProtagonist().getHp());
        assertEquals(0, flow.getProtagonist().getCurrentXp());

        finish(flow);                                        // and the story goes on from there
        assertEquals(new BattleStep(DUMMY), flow.current());
    }

    @Test void goingBackAfterADefeatInTheForestReturnsToTheStepBeforeTheAmbush()
    {
        GameFlow flow = atTheForest(1.0);
        int fullHp = flow.getProtagonist().getStats().maxHealth();

        flow.complete(new Walked());                         // an ambush, with the forest at step 1
        flow.getProtagonist().takeDamage(100);
        flow.complete(new BattleLost());
        flow.complete(new GoBack());

        assertInstanceOf(TrekStep.class, flow.current());
        assertEquals(0, flow.getTrekProgress());
        assertEquals(fullHp, flow.getProtagonist().getHp());
    }

    @Test void afterAVictoryInTheForestGoingBackStartsFromTheNewStep()
    {
        GameFlow flow = atTheForest(1.0);

        flow.complete(new Walked());                         // ambush at step 1
        flow.complete(new BattleWon(Map.of()));
        finish(flow);                                        // the forest goes on: a new safe point
        flow.complete(new Walked());                         // another ambush, at step 2
        flow.complete(new BattleLost());
        flow.complete(new GoBack());

        assertEquals(1, flow.getTrekProgress());
    }

    @Test void theGameCanBePlayedAgainAfterGoingBack()
    {
        GameFlow flow = atTheFirstBattle();
        flow.complete(new BattleLost());
        flow.complete(new GoBack());

        finish(flow);
        flow.complete(new BattleWon(Map.of()));
        finish(flow);

        assertSame(TALK, flow.current());                    // the dialogue after the tutorial
    }

    @Test void goBackIsOnlyAnAnswerToADefeat()
    {
        GameFlow flow = atTheFirstBattle();

        assertThrows(IllegalArgumentException.class, () -> flow.complete(new GoBack()));       // in a battle
        flow.complete(new BattleWon(Map.of()));
        assertThrows(IllegalArgumentException.class, () -> flow.complete(new GoBack()));       // in the reward
        assertInstanceOf(RewardStep.class, flow.current());
    }

    // ---------- Safe points ----------

    @Test void everyNewSafePointIsToldToTheListeners()
    {
        List<GameSnapshot> told = new ArrayList<>();
        GameFlow flow = newFlow(0.0);
        flow.addSafePointListener(told::add);

        flow.newGame();                                      // 1: the first dialogue
        finish(flow);                                        // a battle: nothing new
        flow.complete(new BattleWon(Map.of()));                     // the reward: nothing new
        finish(flow);                                        // 2: the dialogue after
        finish(flow);                                        // 3: the forest
        flow.complete(new Walked());                         // 4: one step further

        assertEquals(List.of("intro", "after", "forest", "forest"), told.stream().map(GameSnapshot::stepId).toList());
        assertEquals(1, told.getLast().trekProgress());
    }

    @Test void anAmbushIsNotASafePoint()
    {
        List<GameSnapshot> told = new ArrayList<>();
        GameFlow flow = atTheForest(1.0);
        flow.addSafePointListener(told::add);

        flow.complete(new Walked());                         // an ambush

        assertTrue(told.isEmpty());
    }

    @Test void goingBackOrLoadingDoesNotTellTheSameSafePointAgain()
    {
        List<GameSnapshot> told = new ArrayList<>();
        GameFlow flow = newFlow(0.0);
        flow.addSafePointListener(told::add);
        flow.newGame();
        GameSnapshot saved = flow.save().orElseThrow();

        finish(flow);
        flow.complete(new BattleLost());
        flow.complete(new GoBack());
        flow.restore(saved);

        assertEquals(1, told.size());
    }

    @Test void aListenerIsRequired()
    {
        assertThrows(NullPointerException.class, () -> newFlow(0).addSafePointListener(null));
    }
}
