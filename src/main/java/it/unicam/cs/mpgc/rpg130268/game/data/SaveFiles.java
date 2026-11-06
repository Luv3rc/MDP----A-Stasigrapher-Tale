package it.unicam.cs.mpgc.rpg130268.game.data;

import java.nio.file.Path;

// Where the saves of the game are kept. It is a folder of the player and not the folder of the program, so the saves
// stay there when the program is moved or updated
public final class SaveFiles
{
    private static final String FOLDER = ".stasigrapher";
    private static final String AUTOSAVE = "autosave.xml";

    private SaveFiles() { }

    public static Path folder() { return Path.of(System.getProperty("user.home"), FOLDER); }

    // The file the autosave writes and the main menu loads
    public static Path autosave() { return folder().resolve(AUTOSAVE); }
}
