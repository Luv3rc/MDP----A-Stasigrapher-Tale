package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilities;
import it.unicam.cs.mpgc.rpg130268.game.MemberState;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemberCatalogTest
{
    private static final WeaponCatalog WEAPONS = WeaponCatalog.fromResource(GameData.WEAPONS_FILE);
    private static final MemberCatalog CATALOG = MemberCatalog.fromResource(GameData.MEMBERS_FILE, WEAPONS, PartyAbilities.standard());

    private static final List<String> ALL_FOUR = List.of("basic-attack", "cuts-for-ink", "stasigraphic-fury", "thirst-for-ink");

    private static MemberCatalog load(String xml)
    {
        return MemberCatalog.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "test.xml", WEAPONS, PartyAbilities.standard());
    }

    // A small valid file where one detail can be changed
    private static String file(String id, String weapon, String abilities)
    {
        return "<members startingParty=\"" + id + "\"><member id=\"" + id + "\" baseHealth=\"164\" healthPerPoint=\"15\" pointsPerLevel=\"3\" statCap=\"60\">"
                + "<ap max=\"9\" start=\"3\"/><weapon ref=\"" + weapon + "\"/><abilities>" + abilities + "</abilities>"
                + "</member></members>";
    }

    private static String learn(String... ids)
    {
        StringBuilder xml = new StringBuilder();
        for (String id : ids) xml.append("<learn ref=\"").append(id).append("\"/>");
        return xml.toString();
    }

    // ---------- The data of the game ----------

    @Test void guntherIsTheMemberOfTheGame()
    {
        assertTrue(CATALOG.contains("gunther"));
        assertFalse(CATALOG.contains("dragon"));
        assertEquals(1, CATALOG.ids().size());
    }

    @Test void aNewGameStartsWithGuntherInTheParty()
    {
        assertEquals(List.of("gunther"), CATALOG.startingParty());
    }

    @Test void aNewGameStartsGuntherAtLevelOneWithThreePointsToSpendAndFullHealth()
    {
        MemberState state = CATALOG.startingState("gunther");

        assertEquals(new MemberState("gunther", 1, 0, 164, StatPoints.NONE, 3, "thesmosia", ALL_FOUR), state);
    }

    @Test void theFileSaysThatGuntherStartsAtLevelOneWithNoPointsInAnyStatAndThreeToSpend()
    {
        PartyMember gunther = CATALOG.newMember("gunther");

        assertEquals(1, gunther.getLevel());
        assertEquals(StatPoints.NONE, gunther.getStatPoints());
        assertEquals(3, gunther.getUnspentPoints());
        assertEquals(new Stats(164, 0, 0, 0, 0), gunther.getStats());
    }

    @Test void theNewGuntherIsWhatTheFileSays()
    {
        PartyMember gunther = CATALOG.newMember("gunther");

        assertInstanceOf(Gunther.class, gunther);
        assertEquals("Gunther", gunther.getName());
        assertEquals(new Stats(164, 0, 0, 0, 0), gunther.getStats());
        assertEquals(164, gunther.getHp());
        assertEquals(1, gunther.getLevel());
        assertEquals(9, gunther.getAp().getMaxAp());
        assertEquals(3, gunther.getAp().getCurrentAp());
        assertEquals(15, gunther.getStatRules().healthPerPoint());
        assertEquals(3, gunther.getStatRules().pointsPerLevel());
        assertSame(WEAPONS.weapon("thesmosia"), gunther.getWeapon());
        assertEquals(ALL_FOUR, gunther.getListOfAbility().stream().map(Ability::getId).toList());
    }

    @Test void everyMemberIsNewAndSharesNothing()
    {
        PartyMember first = CATALOG.newMember("gunther");
        PartyMember second = CATALOG.newMember("gunther");

        assertNotSame(first, second);
        assertNotSame(first.getListOfAbility().getFirst(), second.getListOfAbility().getFirst());
    }

    // ---------- Making a member from his state ----------

    @Test void aMemberFarIntoTheGameComesBackAsHeWas()
    {
        MemberState state = new MemberState("gunther", 5, 120, 100, new StatPoints(2, 4, 0, 6, 1), 3, "thesmosia",
                List.of("basic-attack", "cuts-for-ink"));

        PartyMember gunther = CATALOG.create(state);

        assertEquals(5, gunther.getLevel());
        assertEquals(120, gunther.getCurrentXp());
        assertEquals(new Stats(194, 4, 0, 6, 1), gunther.getStats());
        assertEquals(100, gunther.getHp());
        assertEquals(3, gunther.getUnspentPoints());
        assertEquals(List.of("basic-attack", "cuts-for-ink"), gunther.getListOfAbility().stream().map(Ability::getId).toList());
        assertEquals(state, MemberState.of(gunther));        // and what he is gives that state again
    }

    @Test void aStateThatMakesNoSenseIsRefused()
    {
        MemberState wounded = CATALOG.startingState("gunther");

        assertThrows(GameDataException.class, () -> CATALOG.create(new MemberState("dragon", 1, 0, 10, StatPoints.NONE, 0, "thesmosia", ALL_FOUR)));
        assertThrows(GameDataException.class, () -> CATALOG.create(new MemberState("gunther", 1, 0, 164, StatPoints.NONE, 0, "excalibur", ALL_FOUR)));
        assertThrows(GameDataException.class, () -> CATALOG.create(new MemberState("gunther", 1, 0, 164, StatPoints.NONE, 0, "thesmosia", List.of("fireball"))));
        assertThrows(GameDataException.class, () -> CATALOG.create(new MemberState("gunther", 1, 0, 165, StatPoints.NONE, 0, "thesmosia", ALL_FOUR)));   // over his max HP
        assertEquals(164, wounded.hp());
    }

    // ---------- What a file can say ----------

    @Test void aMemberWithHisOwnNumbers()
    {
        MemberCatalog catalog = load("<members startingParty=\"gunther\"><member id=\"gunther\" baseHealth=\"120\" healthPerPoint=\"8\" pointsPerLevel=\"5\" statCap=\"30\">"
                + "<ap max=\"6\" start=\"2\"/><weapon ref=\"thesmosia\"/><abilities>" + learn("basic-attack") + "</abilities></member></members>");

        PartyMember gunther = catalog.newMember("gunther");

        assertEquals(120, gunther.getStats().maxHealth());
        assertEquals(8, gunther.getStatRules().healthPerPoint());
        assertEquals(5, gunther.getStatRules().pointsPerLevel());
        assertEquals(30, gunther.getStatRules().statCap());
        assertEquals(6, gunther.getAp().getMaxAp());
        assertEquals(1, gunther.getListOfAbility().size());
    }

    @Test void aMemberCanStartWithItsOwnLevelPointsAndPointsToSpend()
    {
        MemberCatalog catalog = load("<members startingParty=\"gunther\"><member id=\"gunther\" level=\"4\" unspentPoints=\"1\""
                + " baseHealth=\"164\" healthPerPoint=\"15\" pointsPerLevel=\"3\" statCap=\"60\">"
                + "<startingPoints health=\"2\" strength=\"1\" agility=\"2\"/>"
                + "<ap max=\"9\" start=\"3\"/><weapon ref=\"thesmosia\"/><abilities>" + learn("basic-attack") + "</abilities></member></members>");

        MemberState state = catalog.startingState("gunther");
        PartyMember gunther = catalog.newMember("gunther");

        assertEquals(new MemberState("gunther", 4, 0, 194, new StatPoints(2, 1, 0, 2, 0), 1, "thesmosia", List.of("basic-attack")), state);
        assertEquals(new Stats(194, 1, 0, 2, 0), gunther.getStats());
        assertEquals(194, gunther.getHp());
        assertEquals(4, gunther.getLevel());
        assertEquals(1, gunther.getUnspentPoints());
    }

    @Test void whatTheFileDoesNotSayAboutTheStartIsLevelOneAndNothing()
    {
        MemberState state = load(file("gunther", "thesmosia", learn("basic-attack"))).startingState("gunther");

        assertEquals(1, state.level());
        assertEquals(0, state.unspentPoints());
        assertEquals(StatPoints.NONE, state.points());
        assertEquals(164, state.hp());
    }

    @Test void aMemberCannotStartWithMorePointsInAStatThanItsCap()
    {
        String xml = "<members startingParty=\"gunther\"><member id=\"gunther\" baseHealth=\"164\" healthPerPoint=\"15\""
                + " pointsPerLevel=\"3\" statCap=\"10\"><startingPoints strength=\"11\"/><ap max=\"9\" start=\"3\"/>"
                + "<weapon ref=\"thesmosia\"/><abilities/></member></members>";

        GameDataException error = assertThrows(GameDataException.class, () -> load(xml));

        assertTrue(error.getMessage().contains("STRENGTH"));
    }

    @Test void startingValuesThatMakeNoSenseAreRefusedByTheSchema()
    {
        assertThrows(GameDataException.class, () -> load(file("gunther", "thesmosia", "").replace("<member id=\"gunther\"", "<member id=\"gunther\" level=\"0\"")));
        assertThrows(GameDataException.class, () -> load(file("gunther", "thesmosia", "").replace("<member id=\"gunther\"", "<member id=\"gunther\" unspentPoints=\"-1\"")));
        assertThrows(GameDataException.class, () -> load(file("gunther", "thesmosia", "").replace("<ap ", "<startingPoints strength=\"-2\"/><ap ")));
    }

    @Test void aMemberCanStartWithNoAbilities()
    {
        assertTrue(load(file("gunther", "thesmosia", "")).newMember("gunther").getListOfAbility().isEmpty());
    }

    // ---------- What a file cannot say ----------

    @Test void aMemberWithNoCodeIsRefusedWhenTheFileIsRead()
    {
        GameDataException e = assertThrows(GameDataException.class, () -> load(file("dragon", "thesmosia", learn("basic-attack"))));

        assertTrue(e.getMessage().contains("dragon"));
    }

    @Test void aWeaponOrAnAbilityThatDoesNotExistIsRefusedWhenTheFileIsRead()
    {
        assertThrows(GameDataException.class, () -> load(file("gunther", "excalibur", learn("basic-attack"))));
        assertThrows(GameDataException.class, () -> load(file("gunther", "thesmosia", learn("fireball"))));
    }

    @Test void anAbilityLearnedTwiceIsRefused()
    {
        GameDataException e = assertThrows(GameDataException.class,
                () -> load(file("gunther", "thesmosia", learn("basic-attack", "basic-attack"))));

        assertTrue(e.getMessage().contains("twice"));
    }

    @Test void aWeaponHeCannotWieldIsRefusedWhenTheFileIsRead()
    {
        WeaponCatalog clubs = WeaponCatalog.load(new ByteArrayInputStream(
                "<weapons><weapon id=\"club\" name=\"Club\" type=\"CLUB\" damage=\"5\"/></weapons>".getBytes(StandardCharsets.UTF_8)), "weapons.xml");

        assertThrows(GameDataException.class, () -> MemberCatalog.load(new ByteArrayInputStream(
                file("gunther", "club", learn("basic-attack")).getBytes(StandardCharsets.UTF_8)), "test.xml", clubs, PartyAbilities.standard()));
    }

    @Test void numbersThatMakeNoSenseAndTwoMembersWithTheSameIdAreRefused()
    {
        assertThrows(GameDataException.class, () -> load(file("gunther", "thesmosia", "").replace("baseHealth=\"164\"", "baseHealth=\"0\"")));
        assertThrows(GameDataException.class, () -> load(file("gunther", "thesmosia", "").replace("</members>",
                "<member id=\"gunther\" baseHealth=\"1\" healthPerPoint=\"1\" pointsPerLevel=\"1\" statCap=\"1\"><ap max=\"1\" start=\"0\"/>"
                + "<weapon ref=\"thesmosia\"/><abilities/></member></members>")));
    }

    @Test void theStartingPartyMustBeMadeOfMembersOfTheFileEachOnceAtLeastOne()
    {
        assertThrows(GameDataException.class, () -> load(file("gunther", "thesmosia", "").replace("startingParty=\"gunther\"", "startingParty=\"dragon\"")));
        assertThrows(GameDataException.class, () -> load(file("gunther", "thesmosia", "").replace("startingParty=\"gunther\"", "startingParty=\"gunther gunther\"")));
        assertThrows(GameDataException.class, () -> load(file("gunther", "thesmosia", "").replace(" startingParty=\"gunther\"", "")));
        assertThrows(GameDataException.class, () -> load(file("gunther", "thesmosia", "").replace("startingParty=\"gunther\"", "startingParty=\"\"")));
    }

    @Test void theCodeOfTheMembersCanBeGivenAndTheOrderOfThePartyIsKept()
    {
        String xml = "<members startingParty=\"squire gunther\">"
                + "<member id=\"gunther\" baseHealth=\"164\" healthPerPoint=\"15\" pointsPerLevel=\"3\" statCap=\"60\"><ap max=\"9\" start=\"3\"/>"
                + "<weapon ref=\"thesmosia\"/><abilities/></member>"
                + "<member id=\"squire\" baseHealth=\"100\" healthPerPoint=\"10\" pointsPerLevel=\"3\" statCap=\"60\"><ap max=\"6\" start=\"2\"/>"
                + "<weapon ref=\"thesmosia\"/><abilities>" + learn("basic-attack") + "</abilities></member></members>";

        MemberCatalog catalog = MemberCatalog.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "test.xml",
                WEAPONS, PartyAbilities.standard(), TestMembers.withSquire());

        assertEquals(List.of("squire", "gunther"), catalog.startingParty());
        assertEquals("Squire", catalog.newMember("squire").getName());
        assertEquals(100, catalog.newMember("squire").getStats().maxHealth());
    }

    @Test void somethingThatIsNotXmlAndAMissingFileAreRefused()
    {
        assertThrows(GameDataException.class, () -> load("not xml"));
        assertThrows(GameDataException.class, () -> MemberCatalog.fromResource("/game/nothing.xml", WEAPONS, PartyAbilities.standard()));
    }
}
