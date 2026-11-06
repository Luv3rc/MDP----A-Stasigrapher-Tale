package it.unicam.cs.mpgc.rpg130268.game.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaveFilesTest
{
    @Test void theAutoSaveIsInTheFolderOfTheSaves()
    {
        assertEquals(SaveFiles.folder(), SaveFiles.autosave().getParent());
        assertEquals("autosave.xml", SaveFiles.autosave().getFileName().toString());
    }

    @Test void theSavesAreInTheFolderOfThePlayerNotOfTheProgram()
    {
        assertTrue(SaveFiles.folder().startsWith(System.getProperty("user.home")));
    }
}
