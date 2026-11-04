package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.FirstUsableAbility;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.PerEnemyBehavior;
import it.unicam.cs.mpgc.rpg130268.game.Encounter;
import it.unicam.cs.mpgc.rpg130268.game.EncounterSetup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static it.unicam.cs.mpgc.rpg130268.game.data.XmlSupport.*;

// All the enemies and the encounters of the game, read from an XML file by id.
// The file is read once, here; every Encounter it gives makes new enemies each time it is spawned
public final class EncounterCatalog
{
    static final String SCHEMA = "/game/enemies.xsd";

    private record EncounterDefinition(String id, String area, List<EnemyDefinition> enemies)
    {
        EncounterSetup spawn()
        {
            List<Enemy> fighters = new ArrayList<>();
            PerEnemyBehavior behavior = new PerEnemyBehavior(new FirstUsableAbility());

            for (EnemyDefinition definition : enemies)
            {
                Enemy enemy = definition.create();
                fighters.add(enemy);
                behavior.assign(enemy, definition.behaviorFor());
            }

            return new EncounterSetup(fighters, behavior, area);
        }
    }

    private final Map<String, EncounterDefinition> encounters;

    private EncounterCatalog(Map<String, EncounterDefinition> encounters) { this.encounters = encounters; }

    // ---------- Reading ----------

    // "/game/enemies.xml"
    public static EncounterCatalog fromResource(String path)
    {
        return load(openResource(path), path);
    }

    public static EncounterCatalog load(InputStream in, String source)
    {
        Document document = parse(in, source, SCHEMA);

        Map<String, EnemyDefinition> enemies = new LinkedHashMap<>();
        for (Element element : children(document.getDocumentElement(), "enemy"))
        {
            EnemyDefinition definition = EnemyDefinition.parse(element);
            enemies.put(definition.id(), definition);       // the schema already forbids two with the same id
        }

        Map<String, EncounterDefinition> encounters = new LinkedHashMap<>();
        for (Element element : children(document.getDocumentElement(), "encounter"))
        {
            List<EnemyDefinition> present = new ArrayList<>();

            for (Element ref : children(element, "enemy"))
                present.add(enemies.get(string(ref, "ref")));   // the schema has checked it exists

            String id = string(element, "id");
            encounters.put(id, new EncounterDefinition(id, string(element, "area"), List.copyOf(present)));
        }

        return new EncounterCatalog(encounters);
    }

    // ---------- Using ----------

    public boolean contains(String id) { return encounters.containsKey(id); }

    public Set<String> ids() { return Set.copyOf(encounters.keySet()); }

    public Encounter encounter(String id)
    {
        EncounterDefinition definition = encounters.get(id);

        if (definition == null)
            throw new GameDataException("there is no encounter '" + id + "'");

        return definition::spawn;
    }
}
