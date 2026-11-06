package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import it.unicam.cs.mpgc.rpg130268.game.GameFlow;
import it.unicam.cs.mpgc.rpg130268.game.GameSnapshot;
import it.unicam.cs.mpgc.rpg130268.game.MemberState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SaveServiceTest
{
    private static final MemberState GUNTHER = new MemberState("gunther", 2, 30, 150, new StatPoints(1, 2, 0, 1, 0), 3,
            "thesmosia", List.of("basic-attack", "cuts-for-ink"));
    private static final MemberState SQUIRE = new MemberState("squire", 5, 0, 80, StatPoints.NONE, 0,
            "thesmosia", List.of());

    private static final GameSnapshot SAVE = new GameSnapshot("forest", 2, List.of(GUNTHER, SQUIRE));

    private static final GameData DATA = GameData.bundled();

    // Runs the test in a folder that is deleted afterwards
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

    // A new save file with the content changed by hand
    private static Path edited(Path folder, String from, String to) throws IOException
    {
        Path file = Files.createTempFile(folder, "edited", ".xml");
        SaveService.write(SAVE, file);
        Files.writeString(file, Files.readString(file).replace(from, to));
        return file;
    }

    // ---------- Writing and reading ----------

    @Test void aSavedGameIsReadBackIdentical() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("save.xml");

            SaveService.write(SAVE, file);

            assertEquals(SAVE, SaveService.read(file));
        });
    }

    @Test void theFileKeepsEveryMemberWithHisPointsHisWeaponAndHisAbilities() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("save.xml");
            SaveService.write(SAVE, file);

            GameSnapshot read = SaveService.read(file);

            assertEquals(List.of("gunther", "squire"), read.party().stream().map(MemberState::id).toList());
            assertEquals(new StatPoints(1, 2, 0, 1, 0), read.party().getFirst().points());
            assertEquals(3, read.party().getFirst().unspentPoints());
            assertEquals("thesmosia", read.party().getFirst().weaponId());
            assertEquals(List.of("basic-attack", "cuts-for-ink"), read.party().getFirst().abilityIds());
            assertTrue(read.party().get(1).abilityIds().isEmpty());
        });
    }

    @Test void theFileIsReadableXmlWithTheVersionAndTheIdOfTheStep() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("save.xml");
            SaveService.write(SAVE, file);

            String text = Files.readString(file, StandardCharsets.UTF_8);

            assertTrue(text.contains("<save version=\"" + SaveService.FORMAT_VERSION + "\">"));
            assertTrue(text.contains("step=\"forest\""));
            assertTrue(text.contains("weapon=\"thesmosia\""));
            assertTrue(text.contains("hp=\"150\""));
        });
    }

    @Test void savingAgainReplacesTheOldSaveAndLeavesNoOtherFiles() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("save.xml");
            SaveService.write(SAVE, file);

            GameSnapshot newer = new GameSnapshot("demo-end", 0, List.of(GUNTHER));
            SaveService.write(newer, file);

            assertEquals(newer, SaveService.read(file));
            try (Stream<Path> files = Files.list(folder))
            {
                assertEquals(1, files.count());
            }
        });
    }

    @Test void theFolderIsCreatedIfItDoesNotExist() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("saves").resolve("slot1.xml");

            SaveService.write(SAVE, file);

            assertTrue(SaveService.exists(file));
        });
    }

    @Test void aFailedWriteLeavesTheOldSaveAndNoTemporaryFile() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("save.xml");
            SaveService.write(SAVE, file);
            Files.delete(file);
            Files.createDirectory(file);                 // now the place of the save is a folder, so it can't be replaced
            Files.writeString(file.resolve("keep.txt"), "this stays");

            assertThrows(GameDataException.class, () -> SaveService.write(SAVE, file));

            assertTrue(Files.isDirectory(file));
            try (Stream<Path> files = Files.list(folder))
            {
                assertEquals(1, files.count());          // only the folder, no save1234.tmp left behind
            }
        });
    }

    // ---------- Reading what is wrong ----------

    @Test void aMissingSaveDoesNotExistAndCantBeRead() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("none.xml");

            assertFalse(SaveService.exists(file));
            assertThrows(GameDataException.class, () -> SaveService.read(file));
        });
    }

    @Test void aDamagedSaveIsRefusedWithAClearMessage() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("save.xml");
            Files.writeString(file, "<save version=\"2\"><position step=\"forest\"");

            GameDataException error = assertThrows(GameDataException.class, () -> SaveService.read(file));

            assertTrue(error.getMessage().contains("save.xml"));
        });
    }

    @Test void aSaveEditedWithWrongValuesIsRefused() throws IOException
    {
        inFolder(folder ->
        {
            assertThrows(GameDataException.class, () -> SaveService.read(edited(folder, "level=\"2\"", "level=\"-4\"")));
            assertThrows(GameDataException.class, () -> SaveService.read(edited(folder, "hp=\"150\"", "hp=\"lots\"")));
            assertThrows(GameDataException.class, () -> SaveService.read(edited(folder, "weapon=\"thesmosia\"", "weapon=\"Thesmosia\"")));
        });
    }

    @Test void aSaveWithTheSameMemberTwiceIsRefused() throws IOException
    {
        inFolder(folder ->
                assertThrows(GameDataException.class, () -> SaveService.read(edited(folder, "id=\"squire\"", "id=\"gunther\""))));
    }

    @Test void aSaveWithAnotherVersionIsRefusedAndTheMessageSaysSo() throws IOException
    {
        inFolder(folder ->
        {
            GameDataException older = assertThrows(GameDataException.class, () -> SaveService.read(edited(folder, "version=\"2\"", "version=\"1\"")));
            GameDataException newer = assertThrows(GameDataException.class, () -> SaveService.read(edited(folder, "version=\"2\"", "version=\"7\"")));

            assertTrue(older.getMessage().contains("another version"));
            assertTrue(older.getMessage().contains("1"));
            assertTrue(newer.getMessage().contains("7"));
        });
    }

    @Test void aSaveOfTheFirstFormatIsRefusedBeforeItsContentIsLookedAt() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("old.xml");
            Files.writeString(file, "<save version=\"1\"><position step=\"forest\" forestProgress=\"2\"/>"
                    + "<gunther level=\"2\" xp=\"30\" hp=\"150\"/></save>");

            GameDataException error = assertThrows(GameDataException.class, () -> SaveService.read(file));

            assertTrue(error.getMessage().contains("another version"));
        });
    }

    @Test void aMissingOrStrangeVersionIsRefusedBySchema() throws IOException
    {
        inFolder(folder ->
        {
            assertThrows(GameDataException.class, () -> SaveService.read(edited(folder, " version=\"2\"", "")));
            assertThrows(GameDataException.class, () -> SaveService.read(edited(folder, "version=\"2\"", "version=\"two\"")));
        });
    }

    @Test void somethingThatIsNotASaveIsRefused() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("other.xml");
            Files.writeString(file, "<weapons/>");

            assertThrows(GameDataException.class, () -> SaveService.read(file));
        });
    }

    // ---------- Loading into a game ----------

    @Test void aSavedGameCanBeLoadedIntoAnotherGame() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("save.xml");
            GameFlow flow = new GameFlow(DATA, () -> 0L);
            flow.getProtagonist().gainXp(200);
            SaveService.write(flow.save().orElseThrow(), file);

            GameFlow other = new GameFlow(DATA, () -> 0L);
            SaveService.load(file, other);

            assertEquals(2, other.getProtagonist().getLevel());
            assertEquals(flow.save(), other.save());
        });
    }

    @Test void aGoodFileThatDoesNotFitTheGameIsRefusedInTheSameWayAndNothingChanges() throws IOException
    {
        inFolder(folder ->
        {
            GameFlow flow = new GameFlow(DATA, () -> 0L);
            GameSnapshot before = flow.save().orElseThrow();
            Path unknownStep = edited(folder, "step=\"forest\"", "step=\"the-moon\"");
            Path unknownWeapon = edited(folder, "weapon=\"thesmosia\"", "weapon=\"excalibur\"");
            Path unknownMember = edited(folder, "id=\"squire\"", "id=\"dragon\"");

            for (Path file : List.of(unknownStep, unknownWeapon, unknownMember))
            {
                GameDataException error = assertThrows(GameDataException.class, () -> SaveService.load(file, flow));
                assertTrue(error.getMessage().contains(file.toString()));
            }

            assertEquals(before, flow.save().orElseThrow());
        });
    }

    @Test void aBrokenFileIsRefusedWhenLoading() throws IOException
    {
        inFolder(folder ->
        {
            Path file = folder.resolve("save.xml");
            Files.writeString(file, "not a save");

            assertThrows(GameDataException.class, () -> SaveService.load(file, new GameFlow(DATA, () -> 0L)));
        });
    }
}
