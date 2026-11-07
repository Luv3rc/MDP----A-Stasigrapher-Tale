package it.unicam.cs.mpgc.rpg130268.ui.fx;

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
import it.unicam.cs.mpgc.rpg130268.ui.menu.MainMenuPresenter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.random.RandomGenerator;

/* *
    The JavaFX application of the game. It opens the window, starts from the main menu and, when a game begins, shows
    the screen of the step that the GameFlow says (showStep) and tells the flow how the step ended. The window,
    the canvas and the keys belong to the SceneHost.

    For now the only steps with a screen are the dialogues. The battles, the reward, the trek are still TODO
* */
public class GameApp extends Application
{
    private SceneHost host;
    private GameData data;
    private GameFlow flow;
    private MainMenuPresenter menu;

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

            // These have no screen yet
            case BattleStep ignored -> showMainMenu();
            case RewardStep ignored -> showMainMenu();
            case TrekStep ignored -> showMainMenu();
            case DefeatStep ignored -> showMainMenu();
            case EndStep ignored -> showMainMenu();
        }
    }

    @Override
    public void stop()
    {
        if (host != null) host.stop();
    }
}
