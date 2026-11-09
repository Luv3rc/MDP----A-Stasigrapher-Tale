package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.game.EncounterSetup;
import it.unicam.cs.mpgc.rpg130268.game.GameFlow;
import it.unicam.cs.mpgc.rpg130268.game.StepOutcome;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.BattleStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.DefeatStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.DialogueStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.EndStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.RewardStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.TrekStep;
import it.unicam.cs.mpgc.rpg130268.game.data.AutoSave;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.GameDataException;
import it.unicam.cs.mpgc.rpg130268.game.data.SaveFiles;
import it.unicam.cs.mpgc.rpg130268.game.data.SaveService;
import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenterFactory;
import it.unicam.cs.mpgc.rpg130268.ui.menu.MainMenuPresenter;
import it.unicam.cs.mpgc.rpg130268.ui.reward.RewardPresenter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

/* *
    The JavaFX application of the game. It opens the window, starts from the main menu and, when a game begins, shows
    the screen of the step that the GameFlow says (showStep) and tells the flow how the step ended. The window,
    the canvas and the keys belong to the SceneHost.

    For now the steps with a screen are the dialogues, the battles, the reward and the defeat. The trek and the end
    don't have one yet, and the game goes back to the main menu when it reaches one of them.
    The steps are listed one by one, with no default, so when a new kind of step appears the compiler says that it
    has no screen yet.
* */
public class GameApp extends Application
{
    private SceneHost host;
    private GameData data;
    private GameFlow flow;
    private MainMenuPresenter menu;               // the menu on view, so that a game that can't be loaded can tell it
    private String battleArea = "";               // where the last battle was, which is the background of its reward

    @Override
    public void start(Stage stage)
    {
        data = GameData.bundled();

        host = new SceneHost(stage, "A Stasigrapher Tale");
        host.start();
        showMainMenu();
    }

    private void showMainMenu()
    {
        menu = new MainMenuPresenter(SaveService.exists(SaveFiles.autosave()));

        host.show(new MainMenuScreen(menu, this::newGame, this::loadGame, Platform::exit));
    }


    // ---------- A game ----------

    // The autosave is listening before the game starts again, so it writes the very first moment too
    private void newGame()
    {
        flow = makeFlow();
        AutoSave.attach(flow, SaveFiles.autosave(), this::autosaveFailed);
        flow.newGame();
        showStep();
    }

    private void loadGame()
    {
        try
        {
            GameFlow loaded = makeFlow();
            SaveService.load(SaveFiles.autosave(), loaded);

            flow = loaded;
            AutoSave.attach(flow, SaveFiles.autosave(), this::autosaveFailed);
            showStep();
        }
        catch (GameDataException e)
        {
            menu.showNotice("The saved game can't be loaded", e.getMessage());
        }
    }

    private GameFlow makeFlow() { return new GameFlow(data, RandomGenerator.getDefault()); }

    private void autosaveFailed(GameDataException error)
    {
        host.showMessage("The automatic save failed. " + error.getMessage());
    }

    private void showStep()
    {
        switch (flow.current())
        {
            case DialogueStep dialogue -> host.show(new DialogueScreen(flow.getProtagonist().getName(), dialogue, () ->
            {
                flow.complete(new StepOutcome.Finished());
                showStep();
            }));

            case BattleStep battle -> showBattle(battle);

            case RewardStep reward -> host.show(new RewardScreen(
                    new RewardPresenter(flow.getParty(), reward.reward()), battleArea, () ->
            {
                flow.complete(new StepOutcome.Finished());
                showStep();
            }));

            // After a lost battle the player decides between the same battle again and the last safe moment
            case DefeatStep ignored -> host.show(new DefeatScreen(battleArea,
                    () ->
                    {
                        flow.complete(new StepOutcome.Retry());
                        showStep();
                    },
                    () ->
                    {
                        flow.complete(new StepOutcome.GoBack());
                        showStep();
                    }));

            // These have no screen yet
            case TrekStep ignored -> showMainMenu();
            case EndStep ignored -> showMainMenu();
        }
    }

    // A battle of the story. The encounter is a recipe, so every time the step is shown (also after a retry) the enemies
    // are new. The party is asked when the battle starts, because after a retry or a load the members are new objects
    private void showBattle(BattleStep step)
    {
        EncounterSetup setup = step.encounter().spawn();
        battleArea = setup.area();

        host.show(new BattleScreen(() -> BattlePresenterFactory.create(flow.getParty(), setup), new BattleExit()
        {
            @Override
            public void onWon(BattleEvent.BattleEnded end)
            {
                // The battle knows its members as objects and the game knows them by id
                Map<String, Integer> xpByMember = new HashMap<>();
                end.xpByMember().forEach((member, xp) -> xpByMember.put(member.getId(), xp));

                flow.complete(new StepOutcome.BattleWon(xpByMember));
                showStep();
            }

            @Override
            public void onLost()
            {
                flow.complete(new StepOutcome.BattleLost());
                showStep();
            }
        }));
    }

    @Override
    public void stop()
    {
        if (host != null) host.stop();
    }
}
