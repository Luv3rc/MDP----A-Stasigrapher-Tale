package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.game.GameFlow;
import it.unicam.cs.mpgc.rpg130268.game.GameSnapshot;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

// The automatic save. It listens to GameFlow (see GameFlow.addSafePointListener) and writes every new safe point in
// the same file, so the main menu always has a game to load. If the file can't be written the game does not stop,
// since it would be unfair to lose a fight because of a full disk. The problem goes to "onFailure", which can tell
// the player.
public final class AutoSave implements Consumer<GameSnapshot>
{
    private final Path file;
    private final Consumer<GameDataException> onFailure;

    public AutoSave(Path file, Consumer<GameDataException> onFailure)
    {
        this.file = Objects.requireNonNull(file, "file must not be null");
        this.onFailure = Objects.requireNonNull(onFailure, "onFailure must not be null");
    }

    // From now on the file is written at every safe point of the flow
    public static AutoSave attach(GameFlow flow, Path file, Consumer<GameDataException> onFailure)
    {
        AutoSave autoSave = new AutoSave(file, onFailure);
        flow.addSafePointListener(autoSave);
        return autoSave;
    }

    @Override
    public void accept(GameSnapshot snapshot)
    {
        try
        {
            SaveService.write(snapshot, file);
        }
        catch (GameDataException e)
        {
            onFailure.accept(e);
        }
    }
}
