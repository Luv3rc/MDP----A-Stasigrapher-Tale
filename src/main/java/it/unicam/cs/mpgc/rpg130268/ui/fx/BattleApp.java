package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.DemoBattles;
import javafx.application.Application;
import javafx.stage.Stage;

/* *
    A window with a demo battle and nothing else, to try the battle on its own. The window, the 16:9 canvas and the
    keys belong to the SceneHost.
* */
public class BattleApp extends Application
{
    private SceneHost host;

    @Override
    public void start(Stage stage)
    {
        String encounter = DemoBattles.encounterFrom(getParameters().getRaw());
        DemoBattles.encounter(encounter);               // an encounter that does not exist is found here, before the window opens

        host = new SceneHost(stage, "A Stasigrapher Tale");
        host.start();
        host.show(new BattleScreen(() -> DemoBattles.create(encounter)));
    }

    @Override
    public void stop()
    {
        if (host != null) host.stop();
    }
}
