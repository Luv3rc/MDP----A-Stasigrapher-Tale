package it.unicam.cs.mpgc.rpg130268.game.data;

// A data file (story, enemies, save) that can't be read. So it can be missing,  not valid XML, it breaks the schema,
// or it says something that makes no sense (an encounter that does not exist)
public class GameDataException extends RuntimeException
{
    public GameDataException(String message) { super(message); }

    public GameDataException(String message, Throwable cause) { super(message, cause); }
}
