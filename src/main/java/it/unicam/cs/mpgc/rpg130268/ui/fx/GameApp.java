package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import javafx.application.Application;
import javafx.stage.Stage;

/* *
    The JavaFX application of the game. It only opens the window and hands it to the GameShell, which starts from the
    main menu. The window, the canvas and the keys belong to the SceneHost, what the player does in it is in the
    GameShell, the game in progress is in the GameSession and the screen of every step is chosen in StepScreens.
    Main starts it (it is a separate class on purpose, see Main).
* */
public class GameApp extends Application
{
    private SceneHost host;

    @Override
    public void start(Stage stage)
    {
        host = new SceneHost(stage, "A Stasigrapher Tale");
        host.start();

        new GameShell(host, GameData.bundled()).showMainMenu();
    }

    @Override
    public void stop()
    {
        if (host != null) host.stop();
    }
}
