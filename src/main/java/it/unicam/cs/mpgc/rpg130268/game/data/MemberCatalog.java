package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilities;
import it.unicam.cs.mpgc.rpg130268.game.MemberState;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static it.unicam.cs.mpgc.rpg130268.game.data.XmlSupport.*;

/* *
    The party members of the game, read from an XML file (see members.xsd), by id. It makes a member (a new one, or
    one that was saved) from his MemberState: it is what a new game, a retry and a load all use.
    A member is two things: the numbers and the starting kit are DATA (members.xml); who he is (his class, his ink...)
    is CODE, picked by his id among the builders (standardBuilders). A new member is a class, a line there and an element in members.xml.
    The file is checked when it is read: a member without code, a weapon or an ability that does not exist, a weapon
    he cannot wield or an ability that is not for him are all refused then, not in the middle of a game.
* */
public final class MemberCatalog
{
    static final String SCHEMA = "/game/members.xsd";

    // Makes the member of the code that goes with an id: he comes with his rules, his points, his AP, his level
    // and nothing learned (what he knows and wields comes from the file or from the save)
    @FunctionalInterface
    public interface MemberBuilder
    {
        PartyMember build(StatRules rules, StatPoints points, int unspentPoints, int maxAp, int startingAp, int level, int xp);
    }

    // The members of the game: who is in the code
    public static Map<String, MemberBuilder> standardBuilders()
    {
        return Map.of("gunther", (rules, points, unspent, maxAp, startingAp, level, xp) ->
                new Gunther(rules, points, unspent, maxAp, startingAp, ApRules.defaults(), InkRules.defaults(), level, xp));
    }

    private final Map<String, MemberDefinition> members;
    private final List<String> startingParty;
    private final Map<String, MemberBuilder> builders;
    private final WeaponCatalog weapons;
    private final PartyAbilities abilities;

    private MemberCatalog(Map<String, MemberDefinition> members, List<String> startingParty,
                          Map<String, MemberBuilder> builders, WeaponCatalog weapons, PartyAbilities abilities)
    {
        this.members = members;
        this.startingParty = startingParty;
        this.builders = builders;
        this.weapons = weapons;
        this.abilities = abilities;
    }

    // ---------- Reading ----------

    // A file in the resources of the program, e.g. "/game/members.xml"
    public static MemberCatalog fromResource(String path, WeaponCatalog weapons, PartyAbilities abilities)
    {
        return load(openResource(path), path, weapons, abilities, standardBuilders());
    }

    // "source" is the name used in the error messages
    public static MemberCatalog load(InputStream in, String source, WeaponCatalog weapons, PartyAbilities abilities)
    {
        return load(in, source, weapons, abilities, standardBuilders());
    }

    // With the members of the code given (another game, or a test, can have other members)
    public static MemberCatalog load(InputStream in, String source, WeaponCatalog weapons, PartyAbilities abilities,
                                     Map<String, MemberBuilder> builders)
    {
        Document document = parse(in, source, SCHEMA);

        Map<String, MemberDefinition> members = new LinkedHashMap<>();
        for (Element element : children(document.getDocumentElement(), "member"))
        {
            MemberDefinition definition = parseMember(element);
            members.put(definition.id(), definition);       // the schema already forbids two with the same id
        }

        List<String> startingParty = List.of(string(document.getDocumentElement(), "startingParty").trim().split("\\s+"));

        MemberCatalog catalog = new MemberCatalog(Map.copyOf(members), startingParty, Map.copyOf(builders), weapons, abilities);

        for (MemberDefinition definition : members.values())
            catalog.check(definition);

        catalog.checkStartingParty(source);

        return catalog;
    }

    // The <member> element: it has already passed the schema
    private static MemberDefinition parseMember(Element member)
    {
        String id = string(member, "id");

        try
        {
            StatRules rules = new StatRules(integer(member, "baseHealth"), integer(member, "healthPerPoint"),
                    integer(member, "pointsPerLevel"), integer(member, "statCap"));

            Element ap = child(member, "ap");
            List<String> abilityIds = children(child(member, "abilities"), "learn").stream()
                    .map(learn -> string(learn, "ref")).toList();

            return new MemberDefinition(id, rules, integer(ap, "max"), integer(ap, "start"),
                    string(child(member, "weapon"), "ref"), abilityIds);
        }
        catch (IllegalArgumentException e)       // a number the game does not accept
        {
            throw new GameDataException("member '" + id + "': " + e.getMessage(), e);
        }
    }

    // What the schema cannot say: that everything the member points to exists and fits him.
    // The easiest way to know it is to make him once
    private void check(MemberDefinition definition)
    {
        if (!builders.containsKey(definition.id()))
            throw new GameDataException("member '" + definition.id() + "' has no code: nobody knows how to make him");

        Set<String> known = new HashSet<>();
        for (String ability : definition.abilityIds())
            if (!known.add(ability))
                throw new GameDataException("member '" + definition.id() + "' learns '" + ability + "' twice");

        newMember(definition.id());
    }

    // The party of a new game must be made of members of the file, each one once
    private void checkStartingParty(String source)
    {
        Set<String> seen = new HashSet<>();

        for (String id : startingParty)
        {
            if (!members.containsKey(id))
                throw new GameDataException(source + ": the starting party has '" + id + "', who is not a member");

            if (!seen.add(id))
                throw new GameDataException(source + ": the starting party has '" + id + "' twice");
        }
    }

    // ---------- Using ----------

    // The members who are in the party when a new game begins, in order (the first one is the one the story is about)
    public List<String> startingParty() { return startingParty; }

    public boolean contains(String id) { return members.containsKey(id); }

    public Set<String> ids() { return Set.copyOf(members.keySet()); }

    // How the member starts a new game: level 1, no xp, nothing spent, full HP, with the weapon and the abilities of the file
    public MemberState startingState(String id)
    {
        MemberDefinition definition = definition(id);

        return new MemberState(id, 1, 0, definition.rules().statsFor(StatPoints.NONE).maxHealth(), StatPoints.NONE, 0,
                definition.weaponId(), definition.abilityIds());
    }

    public PartyMember newMember(String id) { return create(startingState(id)); }

    // The member that this state describes. A weapon or an ability that is not in the data, one he cannot use, or
    // an HP he cannot have are refused. Everything is made new every time: nothing is shared between two members
    public PartyMember create(MemberState state)
    {
        MemberDefinition definition = definition(state.id());

        try
        {
            PartyMember member = builders.get(state.id()).build(definition.rules(), state.points(), state.unspentPoints(),
                    definition.maxAp(), definition.startingAp(), state.level(), state.xp());

            member.equip(weapons.weapon(state.weaponId()));

            for (String ability : state.abilityIds())
                member.learn(abilities.createFor(ability, member));

            member.setHp(state.hp());

            return member;
        }
        catch (IllegalArgumentException e)
        {
            throw new GameDataException("member '" + state.id() + "': " + e.getMessage(), e);
        }
    }

    private MemberDefinition definition(String id)
    {
        MemberDefinition definition = members.get(id);

        if (definition == null)
            throw new GameDataException("there is no member '" + id + "'");

        return definition;
    }
}
