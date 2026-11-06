package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilities;
import it.unicam.cs.mpgc.rpg130268.game.DialogueLine;
import it.unicam.cs.mpgc.rpg130268.game.GameFlow;
import it.unicam.cs.mpgc.rpg130268.game.StepOutcome;
import it.unicam.cs.mpgc.rpg130268.game.Story;
import it.unicam.cs.mpgc.rpg130268.game.StoryNode;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AutoSaveTest
{
    private static final MemberCatalog MEMBERS = MemberCatalog.fromResource(GameData.MEMBERS_FILE,
            WeaponCatalog.fromResource(GameData.WEAPONS_FILE), PartyAbilities.standard());

    private static Story story()
    {
        DialogueStep talk = new DialogueStep(List.of(DialogueLine.narration("...")));

        return new Story(List.of(
                new StoryNode("first", talk),
                new StoryNode("second", talk),
                new StoryNode("end", new EndStep("the end"))));
    }

    private static GameFlow flow() { return new GameFlow(story(), MEMBERS, () -> 0L); }

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

    private interface FolderTest { void run(Path folder) throws IOException; }

    @Test void theLastSafePointIsAlwaysInTheFile() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("autosave.xml");
            GameFlow flow = flow();
            AutoSave.attach(flow, file, error -> fail("it should not fail: " + error));

            flow.newGame();
            assertEquals("first", SaveService.read(file).stepId());

            flow.complete(new StepOutcome.Finished());
            assertEquals("second", SaveService.read(file).stepId());
        });
    }

    @Test void theAutoSaveCanBeLoadedToContinue() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("autosave.xml");
            GameFlow flow = flow();
            AutoSave.attach(flow, file, error -> fail("it should not fail: " + error));
            flow.newGame();
            flow.getProtagonist().gainXp(200);
            flow.complete(new StepOutcome.Finished());

            GameFlow other = flow();
            SaveService.load(file, other);

            assertEquals(flow.save(), other.save());
            assertEquals(2, other.getProtagonist().getLevel());
        });
    }

    @Test void aProblemWritingTheFileDoesNotStopTheGameButIsReported() throws IOException
    {
        inFolder(folder ->
        {
            Path notAFolder = folder.resolve("a-file");
            Files.writeString(notAFolder, "I am a file, not a folder");

            List<GameDataException> problems = new ArrayList<>();
            GameFlow flow = flow();
            AutoSave.attach(flow, notAFolder.resolve("autosave.xml"), problems::add);

            flow.newGame();
            flow.complete(new StepOutcome.Finished());

            assertEquals(2, problems.size());
            assertEquals("second", flow.save().orElseThrow().stepId());      // the game went on anyway
            assertFalse(flow.isOver());
        });
    }

    @Test void theFileAndTheFailureHandlerAreRequired()
    {
        assertThrows(NullPointerException.class, () -> new AutoSave(null, error -> { }));
        assertThrows(NullPointerException.class, () -> new AutoSave(Path.of("x.xml"), null));
    }
}
