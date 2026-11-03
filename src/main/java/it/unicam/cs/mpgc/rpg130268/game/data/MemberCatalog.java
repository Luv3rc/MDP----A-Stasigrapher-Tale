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
    The party members of the game, read from an XML file by id. It makes a member (a new one, or
    one that was saved) from his MemberState--it is what a new game, a retry and a load all use.
    A member is two things: the numbers and the starting kit are DATA (members.xml); who he is (his class, his ink...)
    is CODE, picked by his id in BUILDERS
    The file is checked when it is read
* */
public final class MemberCatalog
{
    static final String SCHEMA = "/game/members.xsd";

    @FunctionalInterface
    private interface Builder
    {
        PartyMember build(MemberDefinition definition, StatPoints points, int unspentPoints, int level, int xp);
    }

    private static final Map<String, Builder> BUILDERS = Map.of(
            "gunther", (definition, points, unspent, level, xp) ->
                    new Gunther(definition.rules(), points, unspent, definition.maxAp(), definition.startingAp(),
                            ApRules.defaults(), InkRules.defaults(), level, xp));

    private final Map<String, MemberDefinition> members;
    private final WeaponCatalog weapons;
    private final PartyAbilities abilities;

    private MemberCatalog(Map<String, MemberDefinition> members, WeaponCatalog weapons, PartyAbilities abilities)
    {
        this.members = members;
        this.weapons = weapons;
        this.abilities = abilities;
    }

    // ---------- Reading ----------

    // A file in the resources of the program, e.g. "/game/members.xml"
    public static MemberCatalog fromResource(String path, WeaponCatalog weapons, PartyAbilities abilities)
    {
        return load(openResource(path), path, weapons, abilities);
    }

    public static MemberCatalog load(InputStream in, String source, WeaponCatalog weapons, PartyAbilities abilities)
    {
        Document document = parse(in, source, SCHEMA);

        Map<String, MemberDefinition> members = new LinkedHashMap<>();
        for (Element element : children(document.getDocumentElement(), "member"))
        {
            MemberDefinition definition = parseMember(element);
            members.put(definition.id(), definition);       // the schema already forbids two with the same id
        }

        MemberCatalog catalog = new MemberCatalog(Map.copyOf(members), weapons, abilities);

        for (MemberDefinition definition : members.values())
            catalog.check(definition);

        return catalog;
    }

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

    // Check what the schema can't, so that everything the member points to exists and fits him
    private void check(MemberDefinition definition)
    {
        if (!BUILDERS.containsKey(definition.id()))
            throw new GameDataException("member '" + definition.id() + "' has no code: nobody knows how to make him");

        Set<String> known = new HashSet<>();
        for (String ability : definition.abilityIds())
            if (!known.add(ability))
                throw new GameDataException("member '" + definition.id() + "' learns '" + ability + "' twice");

        newMember(definition.id());
    }

    // ---------- Using ----------

    public boolean contains(String id) { return members.containsKey(id); }

    public Set<String> ids() { return Set.copyOf(members.keySet()); }

    public MemberState startingState(String id)
    {
        MemberDefinition definition = definition(id);

        return new MemberState(id, 1, 0, definition.rules().statsFor(StatPoints.NONE).maxHealth(), StatPoints.NONE, 0,
                definition.weaponId(), definition.abilityIds());
    }

    public PartyMember newMember(String id) { return create(startingState(id)); }

    // The member that this state describes. A weapon or an ability that is not in the data, one he cannot use, or
    // an HP he cannot have are refused. Everything is made new every time
    public PartyMember create(MemberState state)
    {
        MemberDefinition definition = definition(state.id());

        try
        {
            PartyMember member = BUILDERS.get(state.id()).build(definition, state.points(), state.unspentPoints(),
                    state.level(), state.xp());

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
