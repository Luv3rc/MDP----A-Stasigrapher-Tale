package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.EnemyAction;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilityCatalog;
import it.unicam.cs.mpgc.rpg130268.game.EncounterSetup;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EncounterCatalogTest
{
    private static final EncounterCatalog CATALOG = EncounterCatalog.fromResource(GameData.ENEMIES_FILE);

    private static EncounterCatalog load(String xml)
    {
        return EncounterCatalog.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "test.xml");
    }

    // A small valid file where one detail can be changed
    private static String file(String pattern, String encounterRef)
    {
        return "<enemies>"
                + "<enemy id=\"rat\" name=\"Rat\" level=\"1\" xp=\"5\">"
                + "<stats health=\"10\" strength=\"1\" defense=\"0\" agility=\"1\" luck=\"0\"/>"
                + "<abilities><attack id=\"bite\" name=\"Nasty bite\" target=\"ONE_ENEMY\">"
                + "<hit windUpMs=\"500\" reaction=\"PARRY_OR_DODGE\" weight=\"LIGHT\"/></attack></abilities>"
                + pattern
                + "</enemy>"
                + "<encounter id=\"rats\" area=\"cellar\"><enemy ref=\"" + encounterRef + "\"/></encounter>"
                + "</enemies>";
    }

    private static PartyMember gunther()
    {
        WeaponCatalog weapons = WeaponCatalog.fromResource(GameData.WEAPONS_FILE);

        return MemberCatalog.fromResource(GameData.MEMBERS_FILE, weapons, PartyAbilityCatalog.standard()).newMember("gunther");
    }

    private static List<String> ids(Enemy enemy) { return enemy.getListOfAbility().stream().map(Ability::getId).toList(); }

    private static List<String> names(Enemy enemy) { return enemy.getListOfAbility().stream().map(Ability::getName).toList(); }

    // ---------- The data of the game ----------

    @Test void theEncountersOfTheGameAreThere()
    {
        for (String id : List.of("sariel", "goblin", "bandit", "goblin-and-bandit", "mattias-and-alex"))
            assertTrue(CATALOG.contains(id), id);

        assertFalse(CATALOG.contains("dragon"));
    }

    @Test void everyEncounterMakesNewEnemiesAtFullHealthEachTime()
    {
        for (String id : CATALOG.ids())
        {
            EncounterSetup first = CATALOG.encounter(id).spawn();
            EncounterSetup second = CATALOG.encounter(id).spawn();

            assertFalse(first.enemies().isEmpty(), id);
            assertNotSame(first.enemies().getFirst(), second.enemies().getFirst());

            for (Enemy enemy : first.enemies())
                assertEquals(enemy.getStats().maxHealth(), enemy.getHp());
        }
    }

    @Test void anEnemyHasWhatTheFileSays()
    {
        Enemy goblin = CATALOG.encounter("goblin").spawn().enemies().getFirst();

        assertEquals("Goblin", goblin.getName());
        assertEquals(730, goblin.getStats().maxHealth());
        assertEquals(2, goblin.getLevel());
        assertEquals(130, goblin.getXpReward());
        assertEquals("Club", goblin.getWeapon().name());
        assertEquals(53, goblin.getWeapon().baseDamage());
        assertEquals(List.of("Club swing"), names(goblin));
    }

    @Test void everyAbilityHasAnIdAndTheNameStaysForTheScreen()
    {
        Enemy sariel = CATALOG.encounter("sariel").spawn().enemies().getFirst();

        assertEquals(List.of("quick-slash", "lunge"), ids(sariel));
        assertEquals(List.of("Quick slash", "Lunge"), names(sariel));
    }

    @Test void sarielIsShownAsQuestionMarksButKeepsHerOwnPictures()
    {
        Enemy sariel = CATALOG.encounter("sariel").spawn().enemies().getFirst();

        assertEquals("???", sariel.getName());
        assertEquals("sariel", sariel.getArtName());
    }

    @Test void anEnemyWithoutAnArtNameLooksForItsPicturesWithItsName()
    {
        Enemy goblin = CATALOG.encounter("goblin").spawn().enemies().getFirst();

        assertEquals("Goblin", goblin.getArtName());
    }

    @Test void anEncounterCanHaveManyEnemiesAndAPlace()
    {
        EncounterSetup setup = CATALOG.encounter("goblin-and-bandit").spawn();

        assertEquals(List.of("Goblin", "Bandit"), setup.enemies().stream().map(Enemy::getName).toList());
        assertEquals("forest", setup.area());
    }

    @Test void theTutorialEnemyFollowsItsRotation()
    {
        EncounterSetup setup = CATALOG.encounter("sariel").spawn();
        Enemy sariel = setup.enemies().getFirst();
        PartyMember gunther = gunther();

        List<String> used = List.of(
                abilityOf(setup, sariel, gunther), abilityOf(setup, sariel, gunther), abilityOf(setup, sariel, gunther));

        assertEquals(List.of("quick-slash", "lunge", "quick-slash"), used);
    }

    @Test void aBossChangesItsPatternWhenItIsHurt()
    {
        EncounterSetup setup = CATALOG.encounter("mattias-and-alex").spawn();
        Enemy alex = setup.enemies().get(1);
        PartyMember gunther = gunther();

        assertEquals("attacco-pendel", abilityOf(setup, alex, gunther));

        alex.takeDamage(alex.getStats().maxHealth() * 6 / 10);        // below half of its health

        assertEquals("puzza-della-legge-con-tagli", abilityOf(setup, alex, gunther));
    }

    @Test void anEnemyWithoutPatternUsesTheFirstAbilityItCan()
    {
        EncounterSetup setup = CATALOG.encounter("goblin").spawn();

        assertEquals("club-swing", abilityOf(setup, setup.enemies().getFirst(), gunther()));
    }

    // The id of the ability the enemy chooses
    private static String abilityOf(EncounterSetup setup, Enemy enemy, PartyMember foe)
    {
        EnemyAction action = setup.behavior().choose(enemy, List.of(enemy), List.of(foe)).orElseThrow();
        return action.ability().getId();
    }

    @Test void anAbilityThatAnnouncesItselfKeepsItsText()
    {
        Enemy mattias = CATALOG.encounter("mattias-and-alex").spawn().enemies().getFirst();

        assertTrue(mattias.getListOfAbility().stream()
                .anyMatch(a -> a.getId().equals("heal-allay") && a.getAnnouncement().isPresent()));
    }

    // ---------- The id and the name ----------

    @Test void aPatternFollowsTheIdAndNotTheName()
    {
        EncounterSetup setup = load(file("<pattern><rotation><use ability=\"bite\"/></rotation></pattern>", "rat"))
                .encounter("rats").spawn();

        assertEquals("bite", abilityOf(setup, setup.enemies().getFirst(), gunther()));
        assertEquals(List.of("Nasty bite"), names(setup.enemies().getFirst()));      // the name is only what is shown
    }

    @Test void aPatternThatUsesANameInsteadOfAnIdIsRefused()
    {
        String pattern = "<pattern><rotation><use ability=\"Nasty bite\"/></rotation></pattern>";

        assertThrows(GameDataException.class, () -> load(file(pattern, "rat")));
    }

    @Test void twoAbilitiesOfAnEnemyCannotShareTheSameId()
    {
        String twice = file("", "rat");
        String attack = twice.substring(twice.indexOf("<attack"), twice.indexOf("</attack>") + 9);

        GameDataException error = assertThrows(GameDataException.class, () -> load(twice.replace(attack, attack + attack)));

        assertTrue(error.getMessage().contains("bite"));
    }

    @Test void anAbilityWithoutAnIdOrWithABadOneBreaksTheSchema()
    {
        assertThrows(GameDataException.class, () -> load(file("", "rat").replace(" id=\"bite\"", "")));
        assertThrows(GameDataException.class, () -> load(file("", "rat").replace("id=\"bite\"", "id=\"Bite\"")));
    }

    @Test void theWeaponOfAnEnemyIsWrittenLikeTheOnesOfTheParty()
    {
        String withWeapon = file("", "rat").replace("<abilities>",
                "<weapon name=\"Rusty fork\" type=\"CLUB\" damage=\"4\" strengthGrade=\"C\" extraStat=\"LUCK\" extraGrade=\"B\"/><abilities>");

        Enemy rat = load(withWeapon).encounter("rats").spawn().enemies().getFirst();

        assertEquals("Rusty fork", rat.getWeapon().name());
        assertEquals("rusty-fork", rat.getWeapon().id());
        assertEquals(4, rat.getWeapon().baseDamage());
    }

    @Test void anEnemyWithTheExtraStatButNotItsGradeIsRefused()
    {
        String bad = file("", "rat").replace("<abilities>",
                "<weapon name=\"Fork\" type=\"CLUB\" damage=\"4\" extraStat=\"LUCK\"/><abilities>");

        assertThrows(GameDataException.class, () -> load(bad));
    }

    // ---------- Wrong files ----------

    @Test void aSmallValidFileIsAccepted()
    {
        EncounterCatalog catalog = load(file("", "rat"));

        assertEquals("cellar", catalog.encounter("rats").spawn().area());
    }

    @Test void anEncounterThatDoesNotExistIsAnError()
    {
        assertThrows(GameDataException.class, () -> CATALOG.encounter("dragon"));
    }

    @Test void anEncounterWithAnUnknownEnemyBreaksTheSchema()
    {
        GameDataException error = assertThrows(GameDataException.class, () -> load(file("", "dragon")));

        assertTrue(error.getMessage().contains("test.xml"));
    }

    @Test void aPatternCantUseAnAbilityTheEnemyDoesNotHave()
    {
        String pattern = "<pattern><rotation><use ability=\"roar\"/></rotation></pattern>";

        GameDataException error = assertThrows(GameDataException.class, () -> load(file(pattern, "rat")));

        assertTrue(error.getMessage().contains("roar"));
    }

    @Test void aNumberOutOfRangeBreaksTheSchema()
    {
        assertThrows(GameDataException.class, () -> load(file("", "rat").replace("health=\"10\"", "health=\"0\"")));
        assertThrows(GameDataException.class, () -> load(file("", "rat").replace("LIGHT", "TINY")));
    }

    @Test void aPhaseMustStartBetweenZeroAndOne()
    {
        String pattern = "<pattern><rotation><use ability=\"bite\"/></rotation>"
                + "<below hp=\"1.5\"><rotation><use ability=\"bite\"/></rotation></below></pattern>";

        assertThrows(GameDataException.class, () -> load(file(pattern, "rat")));
    }

    @Test void twoEnemiesCantShareTheSameId()
    {
        String twice = file("", "rat");
        String enemy = twice.substring(twice.indexOf("<enemy id"), twice.indexOf("</enemy>") + 8);

        assertThrows(GameDataException.class, () -> load(twice.replace(enemy, enemy + enemy)));
    }

    @Test void aFileThatIsNotXmlIsAnErrorWithAClearMessage()
    {
        GameDataException error = assertThrows(GameDataException.class, () -> load("this is not xml"));

        assertTrue(error.getMessage().contains("test.xml"));
    }

    @Test void aMissingResourceIsAnError()
    {
        assertThrows(GameDataException.class, () -> EncounterCatalog.fromResource("/game/nothing.xml"));
    }
}
