package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.game.DialogueLine;
import it.unicam.cs.mpgc.rpg130268.game.Encounter;
import it.unicam.cs.mpgc.rpg130268.game.Story;
import it.unicam.cs.mpgc.rpg130268.game.StoryNode;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static it.unicam.cs.mpgc.rpg130268.game.data.XmlSupport.*;

// Reads the story from an XML file (dialogues, battles, treks and the end, in order)
// The battles and the treks only name encounters, which have to be in the catalog
public final class StoryLoader
{
    private static final String SCHEMA = "/game/story.xsd";

    private StoryLoader() { }

    // "/game/story.xml"
    public static Story fromResource(String path, EncounterCatalog catalog)
    {
        return load(openResource(path), path, catalog);
    }

    public static Story load(InputStream in, String source, EncounterCatalog catalog)
    {
        Document document = parse(in, source, SCHEMA);
        List<StoryNode> nodes = new ArrayList<>();

        for (Element element : children(document.getDocumentElement()))
        {
            String id = string(element, "id");

            try
            {
                nodes.add(new StoryNode(id, readStep(element, catalog)));
            }
            catch (IllegalArgumentException e)
            {
                throw new GameDataException(source + ", step '" + id + "': " + e.getMessage(), e);
            }
        }

        try
        {
            return new Story(nodes);
        }
        catch (IllegalArgumentException e)
        {
            throw new GameDataException(source + ": " + e.getMessage(), e);
        }
    }

    private static StoryStep readStep(Element element, EncounterCatalog catalog)
    {
        return switch (element.getLocalName())
        {
            case "dialogue" -> new DialogueStep(string(element, "area"), string(element, "partner"),
                    children(element, "line").stream()
                            .map(line -> new DialogueLine(string(line, "speaker"), text(line))).toList());
            case "battle" -> new BattleStep(encounter(catalog, string(element, "encounter")));
            case "trek" -> new TrekStep(string(element, "area"), integer(element, "length"), decimal(element, "chance"),
                    children(element, "encounter").stream()
                            .map(ref -> encounter(catalog, string(ref, "ref"))).toList());
            case "end" -> new EndStep(text(element));
            default -> throw new GameDataException("unknown step <" + element.getLocalName() + ">");
        };
    }

    private static Encounter encounter(EncounterCatalog catalog, String id)
    {
        return catalog.encounter(id);
    }
}
