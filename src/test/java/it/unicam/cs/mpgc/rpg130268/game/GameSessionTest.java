package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilityCatalog;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.DialogueStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.EndStep;
import it.unicam.cs.mpgc.rpg130268.game.data.EncounterCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.GameDataException;
import it.unicam.cs.mpgc.rpg130268.game.data.MemberCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.SaveService;
import it.unicam.cs.mpgc.rpg130268.game.data.WeaponCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest
{
    // The data of the game with a story of two dialogues, so the saves are easy to read
    private static final GameData DATA = withTwoDialogues();

    private static GameData withTwoDialogues()
    {
        WeaponCatalog weapons = WeaponCatalog.fromResource(GameData.WEAPONS_FILE);
        MemberCatalog members = MemberCatalog.fromResource(GameData.MEMBERS_FILE, weapons, PartyAbilityCatalog.standard());
        EncounterCatalog encounters = EncounterCatalog.fromResource(GameData.ENEMIES_FILE);
        DialogueStep talk = new DialogueStep(List.of(DialogueLine.narration("...")));

        return new GameData(weapons, members, encounters, new Story(List.of(
                new StoryNode("first", talk), new StoryNode("second", talk), new StoryNode("end", new EndStep("the end")))));
    }

    private interface FolderTest { void run(Path folder) throws IOException; }

    private static void inFolder(FolderTest test) throws IOException
    {
        Path folder = Files.createTempDirectory("stasigrapher-test");

        try
        {
            test.run(folder);
        }
        finally
        {
            try (Stream<Path> files = Files.walk(folder))
            {
                files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    private static GameSession session(Path file, List<GameDataException> problems)
    {
        return new GameSession(DATA, file, problems::add, () -> 0L);
    }

    @Test void thereIsNoGameInProgressAtTheStart() throws IOException
    {
        inFolder(folder ->
        {
            GameSession session = session(folder.resolve("autosave.xml"), new ArrayList<>());

            assertFalse(session.isInProgress());
            assertThrows(IllegalStateException.class, session::flow);
            assertFalse(session.hasSave());
        });
    }

    @Test void aNewGameStartsAtTheFirstStepAndSavesTheFirstMoment() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("autosave.xml");
            GameSession session = session(file, new ArrayList<>());

            GameFlow flow = session.newGame();

            assertSame(flow, session.flow());
            assertTrue(session.isInProgress());
            assertTrue(session.hasSave());
            assertEquals("first", SaveService.read(file).stepId());
        });
    }

    @Test void everySafeMomentOfTheGameIsSaved() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("autosave.xml");
            GameSession session = session(file, new ArrayList<>());
            GameFlow flow = session.newGame();

            flow.complete(new StepOutcome.Finished());

            assertEquals("second", SaveService.read(file).stepId());
        });
    }

    @Test void aSavedGameIsLoadedAsItWas() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("autosave.xml");
            GameFlow played = session(file, new ArrayList<>()).newGame();
            played.getProtagonist().gainXp(played.getProtagonist().getXpForNextLevel());
            played.complete(new StepOutcome.Finished());

            GameFlow loaded = session(file, new ArrayList<>()).load();

            assertEquals(played.save(), loaded.save());
            assertEquals(played.getProtagonist().getLevel() , loaded.getProtagonist().getLevel());
        });
    }

    @Test void aLoadedGameKeepsBeingSaved() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("autosave.xml");
            session(file, new ArrayList<>()).newGame();
            GameSession second = session(file, new ArrayList<>());

            GameFlow loaded = second.load();
            loaded.complete(new StepOutcome.Finished());

            assertEquals("second", SaveService.read(file).stepId());
        });
    }

    @Test void aGameThatCannotBeLoadedChangesNothing() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("autosave.xml");
            GameSession session = session(file, new ArrayList<>());
            GameFlow inProgress = session.newGame();
            Files.writeString(file, "this is not a save");

            assertThrows(GameDataException.class, session::load);

            assertSame(inProgress, session.flow());                  // the game in progress is still the one it was
        });
    }

    @Test void loadingWithNoSaveIsAnErrorAndStartsNothing() throws IOException
    {
        inFolder(folder ->
        {
            GameSession session = session(folder.resolve("autosave.xml"), new ArrayList<>());

            assertThrows(GameDataException.class, session::load);

            assertFalse(session.isInProgress());
        });
    }

    @Test void aNewGameReplacesTheOneInProgress() throws IOException
    {
        inFolder(folder ->
        {
            GameSession session = session(folder.resolve("autosave.xml"), new ArrayList<>());
            GameFlow first = session.newGame();
            first.complete(new StepOutcome.Finished());

            GameFlow second = session.newGame();

            assertNotSame(first, second);
            assertSame(second, session.flow());
            assertEquals("first", SaveService.read(folder.resolve("autosave.xml")).stepId());
        });
    }

    @Test void aProblemSavingIsToldButTheGameGoesOn() throws IOException
    {
        inFolder(folder ->
        {
            Path notAFolder = folder.resolve("a-file");
            Files.writeString(notAFolder, "I am a file, not a folder");
            List<GameDataException> problems = new ArrayList<>();
            GameSession session = session(notAFolder.resolve("autosave.xml"), problems);

            GameFlow flow = session.newGame();
            flow.complete(new StepOutcome.Finished());

            assertEquals(2, problems.size());
            assertEquals("second", flow.save().orElseThrow().stepId());      // the game went on anyway
        });
    }

    @Test void nothingCanBeMissing()
    {
        Path file = Path.of("autosave.xml");

        assertThrows(NullPointerException.class, () -> new GameSession(null, file, error -> { }, () -> 0L));
        assertThrows(NullPointerException.class, () -> new GameSession(DATA, null, error -> { }, () -> 0L));
        assertThrows(NullPointerException.class, () -> new GameSession(DATA, file, null, () -> 0L));
        assertThrows(NullPointerException.class, () -> new GameSession(DATA, file, error -> { }, null));
    }
}
