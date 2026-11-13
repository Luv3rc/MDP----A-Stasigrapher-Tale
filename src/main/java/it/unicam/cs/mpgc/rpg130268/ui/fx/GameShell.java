package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.game.GameFlow;
import it.unicam.cs.mpgc.rpg130268.game.GameSession;
import it.unicam.cs.mpgc.rpg130268.game.StepOutcome;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.GameDataException;
import it.unicam.cs.mpgc.rpg130268.game.data.SaveFiles;
import it.unicam.cs.mpgc.rpg130268.ui.StepHandlers;
import it.unicam.cs.mpgc.rpg130268.ui.menu.MainMenuPresenter;
import javafx.application.Platform;

import java.util.Objects;
import java.util.random.RandomGenerator;

/* *
    What the player does in the window, from the main menu to the screen of every step. It decides which screen comes
    next and shows it in the SceneHost, and nothing else. The game itself is in the GameSession and the flow, the
    choice of the screen of a step is in StepScreens, and the application that opens the window is GameApp
* */
final class GameShell implements StepContext
{
    private final SceneHost host;
    private final GameSession session;
    private final StepHandlers<GameScreen> screens;

    private MainMenuPresenter menu;
    private String battleArea = "";

    GameShell(SceneHost host, GameData data)
    {
        this.host = Objects.requireNonNull(host, "host must not be null");
        this.session = new GameSession(data, SaveFiles.autosave(),
                error -> host.showMessage("The automatic save failed. " + error.getMessage()), RandomGenerator.getDefault());

        this.screens = StepScreens.screens(this);
        this.screens.requireAll();
    }

    void showMainMenu() { host.show(mainMenu()); }


    // ---------- What the player can do ----------

    private void newGame()
    {
        session.newGame();
        showStep();
    }

    private void loadGame()
    {
        try
        {
            session.load();
            showStep();
        }
        catch (GameDataException e)
        {
            menu.showNotice("The saved game can't be loaded", e.getMessage());
        }
    }

    private void showStep() { host.show(screens.handle(session.flow().current())); }


    // ---------- What the screens of the steps can ask ----------

    @Override
    public GameFlow flow() { return session.flow(); }

    @Override
    public void complete(StepOutcome outcome)
    {
        session.flow().complete(outcome);
        showStep();
    }

    @Override
    public String battleArea() { return battleArea; }

    @Override
    public void setBattleArea(String area) { battleArea = Objects.requireNonNull(area, "area must not be null"); }

    @Override
    public GameScreen mainMenu()
    {
        menu = new MainMenuPresenter(session.hasSave());

        return new MainMenuScreen(menu, this::newGame, this::loadGame, Platform::exit);
    }
}
