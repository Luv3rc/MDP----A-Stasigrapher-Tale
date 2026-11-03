package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static it.unicam.cs.mpgc.rpg130268.game.data.XmlSupport.*;

// All the weapons of the game that a member can wield, read from an XML file (see weapons.xsd), by id.
// A save says "thesmosia". A Weapon never changes, so the same one can be given to everybody who wields it
public final class WeaponCatalog
{
    static final String SCHEMA = "/game/weapons.xsd";

    private final Map<String, Weapon> weapons;

    private WeaponCatalog(Map<String, Weapon> weapons) { this.weapons = weapons; }

    // ---------- Reading ----------

    public static WeaponCatalog fromResource(String path)
    {
        return load(openResource(path), path);
    }

    // "source" is the name used in the error messages
    public static WeaponCatalog load(InputStream in, String source)
    {
        Document document = parse(in, source, SCHEMA);

        Map<String, Weapon> weapons = new LinkedHashMap<>();

        for (Element element : children(document.getDocumentElement(), "weapon"))
        {
            Weapon weapon = WeaponReader.read(element);
            weapons.put(weapon.id(), weapon);       // the schema already forbids two with the same id
        }

        return new WeaponCatalog(weapons);
    }

    // ---------- Using ----------

    public boolean contains(String id) { return weapons.containsKey(id); }

    public Set<String> ids() { return Set.copyOf(weapons.keySet()); }

    public Weapon weapon(String id)
    {
        Weapon weapon = weapons.get(id);

        if (weapon == null)
            throw new GameDataException("there is no weapon '" + id + "'");

        return weapon;
    }
}
