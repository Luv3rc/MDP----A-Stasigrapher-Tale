package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.game.DialogueLine;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.*;
import it.unicam.cs.mpgc.rpg130268.game.Story;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StoryLoaderTest
{
    private static final EncounterCatalog CATALOG = EncounterCatalog.fromResource(GameData.ENEMIES_FILE);

    private static Story load(String xml)
    {
        return StoryLoader.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "story-test.xml", CATALOG);
    }

    private static Story theStory() { return StoryLoader.fromResource(GameData.STORY_FILE, CATALOG); }

    private static final String END = "<end id='end'>Bye</end>";

    // ---------- The story of the game ----------

    @Test void theStoryOfTheGameIsLoadedInOrder()
    {
        Story story = theStory();

        assertInstanceOf(DialogueStep.class, story.node(0).step());
        assertInstanceOf(BattleStep.class, story.node(1).step());
        assertInstanceOf(DialogueStep.class, story.node(2).step());
        assertInstanceOf(TrekStep.class, story.node(3).step());
        assertInstanceOf(EndStep.class, story.node(4).step());
        assertEquals("tutorial", story.node(1).id());
    }

    @Test void theDialoguesKeepSpeakersAndNarrations()
    {
        DialogueStep intro = (DialogueStep) theStory().node(0).step();

        assertEquals("Gunther", intro.lines().get(0).speaker());
        assertEquals("Pant... pant... pant...", intro.lines().get(0).text());
    }

    @Test void aDialogueKnowsItsBackgroundAndWhoTheProtagonistTalksWith()
    {
        DialogueStep intro = (DialogueStep) theStory().node(0).step();

        assertEquals("road", intro.area());
        assertEquals("", intro.partner());
    }

    @Test void theBackgroundAndThePartnerOfADialogueAreOptional()
    {
        DialogueStep step = (DialogueStep) load("<story><dialogue id='a'><line>Hi</line></dialogue>" + END + "</story>").node(0).step();

        assertEquals("", step.area());
        assertEquals("", step.partner());
    }

    @Test void theForestIsATrekWithItsAreaLengthChanceAndEncounters()
    {
        TrekStep forest = (TrekStep) theStory().node(3).step();

        assertEquals("forest", theStory().node(3).id());
        assertEquals("forest", forest.area());
        assertEquals(5, forest.length());
        assertEquals(0.5, forest.encounterChance());
        assertEquals(2, forest.encounters().size());
    }

    @Test void theEncountersOfTheStoryHaveTheirOwnBackgrounds()
    {
        TrekStep forest = (TrekStep) theStory().node(3).step();

        for (var encounter : forest.encounters())
            assertEquals("forest", encounter.spawn().area());
    }

    // ---------- Other stories ----------

    @Test void aTrekCanBeAnyPlace()
    {
        TrekStep cave = (TrekStep) load("<story><trek id='t' area='cave' length='3' chance='1'>"
                + "<encounter ref='goblin'/></trek>" + END + "</story>").node(0).step();

        assertEquals("cave", cave.area());
        assertEquals(3, cave.length());
    }

    @Test void aSmallStoryIsLoaded()
    {
        Story story = load("<story><dialogue id='a'><line speaker='X'>Hi</line></dialogue>"
                + "<battle id='b' encounter='goblin'/>" + END + "</story>");

        assertEquals(3, story.nodes().size());
        assertEquals(new DialogueLine("X", "Hi"), ((DialogueStep) story.node(0).step()).lines().getFirst());
    }

    // ---------- Wrong files ----------

    @Test void aBattleWithAnEncounterThatDoesNotExistIsAnError()
    {
        assertThrows(GameDataException.class, () -> load("<story><battle id='b' encounter='dragon'/>" + END + "</story>"));
    }

    @Test void aTrekWithAnEncounterThatDoesNotExistIsAnError()
    {
        assertThrows(GameDataException.class, () -> load("<story><trek id='t' area='cave' length='3' chance='0.5'>"
                + "<encounter ref='dragon'/></trek>" + END + "</story>"));
    }

    @Test void aTrekWithoutAnAreaBreaksTheSchema()
    {
        assertThrows(GameDataException.class, () -> load("<story><trek id='t' length='3' chance='0'/>" + END + "</story>"));
    }

    @Test void aTrekThatCanMeetNobodyButHasAChanceIsAnError()
    {
        GameDataException error = assertThrows(GameDataException.class,
                () -> load("<story><trek id='t' area='cave' length='3' chance='0.5'/>" + END + "</story>"));

        assertTrue(error.getMessage().contains("'t'"));
    }

    @Test void theOldNameOfTheTrekIsNotAnElementAnymore()
    {
        assertThrows(GameDataException.class, () -> load("<story><forest id='f' length='3' chance='0'/>" + END + "</story>"));
    }

    @Test void theIdsOfTheStepsCantRepeat()
    {
        assertThrows(GameDataException.class, () -> load(
                "<story><dialogue id='a'><line>1</line></dialogue><dialogue id='a'><line>2</line></dialogue>" + END + "</story>"));
    }

    @Test void theStoryMustFinishWithTheEnd()
    {
        assertThrows(GameDataException.class, () -> load("<story><dialogue id='a'><line>1</line></dialogue></story>"));
    }

    @Test void aChanceOutsideZeroAndOneBreaksTheSchema()
    {
        assertThrows(GameDataException.class, () -> load("<story><trek id='t' area='cave' length='3' chance='1.5'>"
                + "<encounter ref='goblin'/></trek>" + END + "</story>"));
    }

    @Test void anUnknownElementBreaksTheSchema()
    {
        assertThrows(GameDataException.class, () -> load("<story><shop id='s'/>" + END + "</story>"));
    }

    @Test void theMessageSaysWhichFileAndWhere()
    {
        GameDataException error = assertThrows(GameDataException.class, () -> load("<story><shop id='s'/>" + END + "</story>"));

        assertTrue(error.getMessage().contains("story-test.xml"));
        assertTrue(error.getMessage().contains("line"));
    }
}
