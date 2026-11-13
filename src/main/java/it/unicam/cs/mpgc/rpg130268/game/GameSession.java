package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.game.data.AutoSave;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.GameDataException;
import it.unicam.cs.mpgc.rpg130268.game.data.SaveService;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/* *
    The game that the player is in, and the things around it that are not the story. It makes the GameFlow of a new
    game or of a saved one, and keeps the automatic save listening to it
    The screens ask it for the flow and tell the flow how the steps end

     If the automatic save can't be written the game goes on, the player is warned
* */
public final class GameSession
{
    private final GameData data;
    private final Path autosave;
    private final Consumer<GameDataException> onAutosaveFailure;
    private final RandomGenerator random;

    private GameFlow flow;

    public GameSession(GameData data, Path autosave, Consumer<GameDataException> onAutosaveFailure, RandomGenerator random)
    {
        this.data = Objects.requireNonNull(data, "data must not be null");
        this.autosave = Objects.requireNonNull(autosave, "autosave must not be null");
        this.onAutosaveFailure = Objects.requireNonNull(onAutosaveFailure, "onAutosaveFailure must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    public boolean hasSave() { return SaveService.exists(autosave); }

    public GameFlow newGame()
    {
        GameFlow fresh = makeFlow();
        AutoSave.attach(fresh, autosave, onAutosaveFailure);
        fresh.newGame();

        flow = fresh;
        return flow;
    }

    public GameFlow load()
    {
        GameFlow loaded = makeFlow();
        SaveService.load(autosave, loaded);
        AutoSave.attach(loaded, autosave, onAutosaveFailure);

        flow = loaded;
        return flow;
    }

    public boolean isInProgress() { return flow != null; }

    public GameFlow flow()
    {
        if (flow == null)
            throw new IllegalStateException("there is no game in progress");

        return flow;
    }

    private GameFlow makeFlow() { return new GameFlow(data, random); }
}
