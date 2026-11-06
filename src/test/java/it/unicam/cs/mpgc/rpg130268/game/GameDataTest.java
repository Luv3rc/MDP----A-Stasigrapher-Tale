package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.game.StepOutcome.*;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.*;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

// The data of the game together: the files must fit each other, and the whole story must be playable
class GameDataTest
{
    private static final GameData DATA = GameData.bundled();

    @Test void theDataOfTheGameLoadsTogether()
    {
        assertTrue(DATA.weapons().contains("thesmosia"));
        assertTrue(DATA.members().contains("gunther"));
        assertTrue(DATA.encounters().contains("sariel"));
        assertTrue(DATA.story().contains("forest"));
    }

    // Plays the story as the UI would, winning every battle: it must reach the end
    private static void playToTheEnd(RandomGenerator dice)
    {
        GameFlow flow = new GameFlow(DATA, dice);
        int guard = 0;

        while (!flow.isOver())
        {
            assertTrue(++guard < 500, "the story does not end");

            switch (flow.current())
            {
                case DialogueStep ignored -> flow.complete(new Finished());
                case RewardStep ignored -> flow.complete(new Finished());
                case TrekStep ignored -> flow.complete(new Walked());
                case BattleStep battle ->
                {
                    // the xp the enemies give, all to the protagonist (the party has only him)
                    int xp = battle.encounter().spawn().enemies().stream().mapToInt(Enemy::getXpReward).sum();
                    flow.getProtagonist().gainXp(xp);
                    flow.complete(new BattleWon(Map.of(flow.getProtagonist().getId(), xp)));
                }
                case DefeatStep ignored -> fail("nobody loses in this test");
                case EndStep ignored -> fail("it is over");
            }
        }
    }

    @Test void theWholeStoryCanBePlayedWithNoAmbushes()
    {
        playToTheEnd(() -> -1L);          // the biggest numbers: no ambush ever
    }

    @Test void theWholeStoryCanBePlayedWithAnAmbushAtEveryStep()
    {
        playToTheEnd(() -> 0L);           // the smallest numbers: an ambush at every step but the last
    }

    @Test void theWholeStoryCanBePlayedWithRealDice()
    {
        for (int i = 0; i < 20; i++)
            playToTheEnd(RandomGenerator.getDefault());
    }

    @Test void everyEncounterOfTheStoryIsInTheCatalog()
    {
        for (StoryNode node : DATA.story().nodes())
            switch (node.step())
            {
                case BattleStep battle -> assertFalse(battle.encounter().spawn().enemies().isEmpty());
                case TrekStep trek -> trek.encounters().forEach(encounter -> assertFalse(encounter.spawn().enemies().isEmpty()));
                default -> { }
            }
    }
}
