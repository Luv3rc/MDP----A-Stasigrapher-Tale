package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.damage.Scaling;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WeaponCatalogTest
{
    private static final WeaponCatalog CATALOG = WeaponCatalog.fromResource(GameData.WEAPONS_FILE);

    private static WeaponCatalog load(String xml)
    {
        return WeaponCatalog.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "test.xml");
    }

    private static String file(String weapons) { return "<weapons>" + weapons + "</weapons>"; }

    // ---------- The data of the game ----------

    @Test void thesmosiaIsTheWeaponOfTheGame()
    {
        assertTrue(CATALOG.contains("thesmosia"));
        assertFalse(CATALOG.contains("excalibur"));

        Weapon expected = new Weapon("thesmosia", "Thesmosia", WeaponType.SWORD, 32,
                Scaling.of(ScalingStat.STRENGTH, ScalingGrade.NONE).and(ScalingStat.AGILITY, ScalingGrade.D));

        assertEquals(expected, CATALOG.weapon("thesmosia"));
    }

    @Test void aWeaponAskedTwiceIsTheSameOne()
    {
        assertSame(CATALOG.weapon("thesmosia"), CATALOG.weapon("thesmosia"));       // it never changes: it can be shared
    }

    @Test void aWeaponThatDoesNotExistIsRefusedWithItsId()
    {
        GameDataException e = assertThrows(GameDataException.class, () -> CATALOG.weapon("excalibur"));

        assertTrue(e.getMessage().contains("excalibur"));
    }

    // ---------- What a file can say ----------

    @Test void aWeaponScalesWithStrengthAndWithOneMoreStat()
    {
        WeaponCatalog catalog = load(file(
                "<weapon id=\"iron-sword\" name=\"Iron Sword\" type=\"SWORD\" damage=\"15\" strengthGrade=\"C\"/>"
                + "<weapon id=\"night-dagger\" name=\"Night Dagger\" type=\"DAGGER\" damage=\"8\" strengthGrade=\"D\""
                + " extraStat=\"LUCK\" extraGrade=\"A\"/>"));

        assertEquals(new Weapon("iron-sword", "Iron Sword", WeaponType.SWORD, 15, Scaling.of(ScalingStat.STRENGTH, ScalingGrade.C)),
                catalog.weapon("iron-sword"));
        assertEquals(new Weapon("night-dagger", "Night Dagger", WeaponType.DAGGER, 8,
                Scaling.of(ScalingStat.STRENGTH, ScalingGrade.D).and(ScalingStat.LUCK, ScalingGrade.A)),
                catalog.weapon("night-dagger"));
        assertEquals(2, catalog.ids().size());
    }

    @Test void withoutAStrengthGradeTheWeaponDoesNotScaleWithStrength()
    {
        WeaponCatalog catalog = load(file("<weapon id=\"stick\" name=\"Stick\" type=\"CLUB\" damage=\"3\"/>"));

        assertEquals(Scaling.none(), catalog.weapon("stick").scaling());
    }

    @Test void theNameCanBeAnythingAndTheIdStaysTheSame()
    {
        WeaponCatalog catalog = load(file("<weapon id=\"thesmosia\" name=\"The Chained Blade\" type=\"SWORD\" damage=\"32\"/>"));

        assertEquals("thesmosia", catalog.weapon("thesmosia").id());
        assertEquals("The Chained Blade", catalog.weapon("thesmosia").name());
    }

    // ---------- What a file cannot say ----------

    @Test void aBadIdIsRefusedByTheSchemaAndTheMessageSaysWhere()
    {
        GameDataException e = assertThrows(GameDataException.class, () ->
                load(file("<weapon id=\"Thesmosia\" name=\"Thesmosia\" type=\"SWORD\" damage=\"32\"/>")));

        assertTrue(e.getMessage().contains("test.xml"));
    }

    @Test void twoWeaponsWithTheSameIdAreRefused()
    {
        assertThrows(GameDataException.class, () -> load(file(
                "<weapon id=\"club\" name=\"Club\" type=\"CLUB\" damage=\"5\"/>"
                + "<weapon id=\"club\" name=\"Other club\" type=\"CLUB\" damage=\"6\"/>")));
    }

    @Test void anExtraStatNeedsItsGradeAndTheOtherWayAround()
    {
        GameDataException e = assertThrows(GameDataException.class, () ->
                load(file("<weapon id=\"club\" name=\"Club\" type=\"CLUB\" damage=\"5\" extraStat=\"LUCK\"/>")));
        assertTrue(e.getMessage().contains("go together"));

        assertThrows(GameDataException.class, () ->
                load(file("<weapon id=\"club\" name=\"Club\" type=\"CLUB\" damage=\"5\" extraGrade=\"B\"/>")));
    }

    @Test void theExtraStatCannotBeStrengthAgain()
    {
        assertThrows(GameDataException.class, () -> load(file(
                "<weapon id=\"club\" name=\"Club\" type=\"CLUB\" damage=\"5\" strengthGrade=\"B\""
                + " extraStat=\"STRENGTH\" extraGrade=\"A\"/>")));
    }

    @Test void aNegativeDamageAnUnknownTypeAndAnEmptyFileAreRefused()
    {
        assertThrows(GameDataException.class, () ->
                load(file("<weapon id=\"club\" name=\"Club\" type=\"CLUB\" damage=\"-1\"/>")));
        assertThrows(GameDataException.class, () ->
                load(file("<weapon id=\"club\" name=\"Club\" type=\"BOW\" damage=\"5\"/>")));
        assertThrows(GameDataException.class, () -> load("<weapons/>"));
    }

    @Test void somethingThatIsNotXmlAndAMissingFileAreRefused()
    {
        assertThrows(GameDataException.class, () -> load("this is not xml"));
        assertThrows(GameDataException.class, () -> WeaponCatalog.fromResource("/game/nothing.xml"));
    }
}
