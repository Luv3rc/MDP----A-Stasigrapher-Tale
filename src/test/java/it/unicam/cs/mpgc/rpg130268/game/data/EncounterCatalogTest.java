package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.EnemyAction;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilityCatalog;
import it.unicam.cs.mpgc.rpg130268.game.EncounterSetup;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/* *
    What the loader does is tested with small files written here, so the numbers and the names of the game
    (which will be tuned many times) are not in the assertions. The real file is only asked for what has to be true
    whatever its numbers are.
* */
class EncounterCatalogTest
{
    private static final EncounterCatalog CATALOG = EncounterCatalog.fromResource(GameData.ENEMIES_FILE);

    private static EncounterCatalog load(String xml)
    {
        return EncounterCatalog.load(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "test.xml");
    }

    // ---------- Small files to write the cases ----------

    private static final String STATS = "<stats health=\"10\" strength=\"1\" defense=\"0\" agility=\"1\" luck=\"0\"/>";

    private static String hit(String reaction, String weight)
    {
        return "<hit windUpMs=\"500\" reaction=\"" + reaction + "\" weight=\"" + weight + "\"/>";
    }

    private static String attack(String id, String name) { return attack(id, name, ""); }

    private static String attack(String id, String name, String extra)
    {
        return "<attack id=\"" + id + "\" name=\"" + name + "\" target=\"ONE_ENEMY\"" + extra + ">" + hit("PARRY_OR_DODGE", "LIGHT") + "</attack>";
    }

    private static String enemy(String id, String name, String extraAttributes, String abilities, String pattern)
    {
        return "<enemy id=\"" + id + "\" name=\"" + name + "\" level=\"3\" xp=\"40\" " + extraAttributes + ">" + STATS
                + "<abilities>" + abilities + "</abilities>" + pattern + "</enemy>";
    }

    private static String encounter(String id, String area, String... enemies)
    {
        StringBuilder xml = new StringBuilder("<encounter id=\"" + id + "\" area=\"" + area + "\">");
        for (String enemy : enemies) xml.append("<enemy ref=\"").append(enemy).append("\"/>");
        return xml.append("</encounter>").toString();
    }

    private static String rotation(String... ids)
    {
        StringBuilder xml = new StringBuilder("<rotation>");
        for (String id : ids) xml.append("<use ability=\"").append(id).append("\"/>");
        return xml.append("</rotation>").toString();
    }

    // The game with one rat that bites (the "rats" encounter), and a detail that can be changed
    private static String file(String pattern, String encounterRef)
    {
        return "<enemies>" + enemy("rat", "Rat", "", attack("bite", "Nasty bite"), pattern) + encounter("rats", "cellar", encounterRef) + "</enemies>";
    }

    private static PartyMember gunther()
    {
        WeaponCatalog weapons = WeaponCatalog.fromResource(GameData.WEAPONS_FILE);

        return MemberCatalog.fromResource(GameData.MEMBERS_FILE, weapons, PartyAbilityCatalog.standard()).newMember("gunther");
    }

    private static List<String> ids(Enemy enemy) { return enemy.getListOfAbility().stream().map(Ability::getId).toList(); }

    private static List<String> names(Enemy enemy) { return enemy.getListOfAbility().stream().map(Ability::getName).toList(); }

    // The id of the ability the enemy chooses
    private static String abilityOf(EncounterSetup setup, Enemy enemy, PartyMember foe)
    {
        EnemyAction action = setup.behavior().choose(enemy, List.of(enemy), List.of(foe)).orElseThrow();
        return action.ability().getId();
    }

    private static EncounterSetup spawn(String xml, String encounterId) { return load(xml).encounter(encounterId).spawn(); }

    // ---------- The game's own file, whatever its numbers are ----------

    @Test void theFileOfTheGameHasEncountersAndEveryOneMakesNewEnemiesAtFullHealthEachTime()
    {
        assertFalse(CATALOG.ids().isEmpty());

        for (String id : CATALOG.ids())
        {
            EncounterSetup first = CATALOG.encounter(id).spawn();
            EncounterSetup second = CATALOG.encounter(id).spawn();

            assertFalse(first.enemies().isEmpty(), id);
            assertFalse(first.area().isBlank(), id);
            assertNotSame(first.enemies().getFirst(), second.enemies().getFirst());

            for (Enemy enemy : first.enemies())
                assertEquals(enemy.getStats().maxHealth(), enemy.getHp());
        }
    }

    @Test void everyEnemyOfTheGameHasAbilitiesWithAnIdOfItsOwn()
    {
        for (String id : CATALOG.ids())
            for (Enemy enemy : CATALOG.encounter(id).spawn().enemies())
            {
                assertFalse(enemy.getListOfAbility().isEmpty(), enemy.getName());
                assertEquals(ids(enemy).size(), ids(enemy).stream().distinct().count(), enemy.getName());
            }
    }

    @Test void everyEnemyOfTheGameChoosesSomethingToDoAgainstAnAliveParty()
    {
        for (String id : CATALOG.ids())
        {
            EncounterSetup setup = CATALOG.encounter(id).spawn();

            for (Enemy enemy : setup.enemies())
                assertTrue(setup.behavior().choose(enemy, new ArrayList<Fighter>(setup.enemies()), List.of(gunther())).isPresent(), enemy.getName());
        }
    }

    @Test void anEncounterThatDoesNotExistIsAnError()
    {
        assertThrows(GameDataException.class, () -> CATALOG.encounter("no-such-encounter"));
        assertFalse(CATALOG.contains("no-such-encounter"));
    }

    // ---------- What a file can say about an enemy ----------

    @Test void anEnemyHasWhatTheFileSays()
    {
        String xml = "<enemies>" + enemy("rat", "Rat", "", attack("bite", "Nasty bite"), "")
                .replace("<abilities>", "<weapon name=\"Rusty fork\" type=\"CLUB\" damage=\"4\" strengthGrade=\"C\"/><abilities>")
                + encounter("rats", "cellar", "rat") + "</enemies>";

        Enemy rat = spawn(xml, "rats").enemies().getFirst();

        assertEquals("Rat", rat.getName());
        assertEquals(10, rat.getStats().maxHealth());
        assertEquals(3, rat.getLevel());
        assertEquals(40, rat.getXpReward());
        assertEquals("Rusty fork", rat.getWeapon().name());
        assertEquals(4, rat.getWeapon().baseDamage());
        assertEquals(List.of("Nasty bite"), names(rat));
    }

    @Test void anEnemyWithoutAWeaponIsUnarmed()
    {
        Enemy rat = spawn(file("", "rat"), "rats").enemies().getFirst();

        assertEquals("Unarmed", rat.getWeapon().name());
    }

    @Test void everyAbilityHasAnIdAndTheNameStaysForTheScreen()
    {
        String xml = "<enemies>" + enemy("rat", "Rat", "", attack("bite", "Nasty bite") + attack("scratch", "Quick scratch"), "")
                + encounter("rats", "cellar", "rat") + "</enemies>";

        Enemy rat = spawn(xml, "rats").enemies().getFirst();

        assertEquals(List.of("bite", "scratch"), ids(rat));
        assertEquals(List.of("Nasty bite", "Quick scratch"), names(rat));
    }

    @Test void anEnemyShownWithAnotherNameKeepsItsOwnPictures()
    {
        String xml = "<enemies>" + enemy("rat", "???", "art=\"rat-art\"", attack("bite", "Nasty bite"), "")
                + encounter("rats", "cellar", "rat") + "</enemies>";

        Enemy rat = spawn(xml, "rats").enemies().getFirst();

        assertEquals("???", rat.getName());
        assertEquals("rat-art", rat.getArtName());
    }

    @Test void anEnemyWithoutAnArtNameLooksForItsPicturesWithItsName()
    {
        assertEquals("Rat", spawn(file("", "rat"), "rats").enemies().getFirst().getArtName());
    }

    @Test void anEncounterCanHaveManyEnemiesAndAPlace()
    {
        String xml = "<enemies>" + enemy("rat", "Rat", "", attack("bite", "Nasty bite"), "")
                + enemy("bat", "Bat", "", attack("flap", "Flap"), "") + encounter("pests", "belfry", "rat", "bat") + "</enemies>";

        EncounterSetup setup = spawn(xml, "pests");

        assertEquals(List.of("Rat", "Bat"), setup.enemies().stream().map(Enemy::getName).toList());
        assertEquals("belfry", setup.area());
    }

    // ---------- Patterns ----------

    @Test void anEnemyFollowsItsRotation()
    {
        String xml = "<enemies>" + enemy("rat", "Rat", "", attack("bite", "Nasty bite") + attack("scratch", "Quick scratch"),
                "<pattern>" + rotation("bite", "scratch") + "</pattern>") + encounter("rats", "cellar", "rat") + "</enemies>";
        EncounterSetup setup = spawn(xml, "rats");
        Enemy rat = setup.enemies().getFirst();
        PartyMember gunther = gunther();

        List<String> used = List.of(abilityOf(setup, rat, gunther), abilityOf(setup, rat, gunther), abilityOf(setup, rat, gunther));

        assertEquals(List.of("bite", "scratch", "bite"), used);
    }

    @Test void aBossChangesItsPatternWhenItIsHurt()
    {
        String xml = "<enemies>" + enemy("boss", "Boss", "", attack("slow", "Slow") + attack("fury", "Fury"),
                "<pattern>" + rotation("slow") + "<below hp=\"0.5\">" + rotation("fury") + "</below></pattern>")
                + encounter("boss-fight", "arena", "boss") + "</enemies>";
        EncounterSetup setup = spawn(xml, "boss-fight");
        Enemy boss = setup.enemies().getFirst();
        PartyMember gunther = gunther();

        assertEquals("slow", abilityOf(setup, boss, gunther));

        boss.takeDamage(boss.getStats().maxHealth() * 6 / 10);        // below half of its health

        assertEquals("fury", abilityOf(setup, boss, gunther));
    }

    @Test void anEnemyWithoutPatternUsesTheFirstAbilityItCan()
    {
        String xml = "<enemies>" + enemy("rat", "Rat", "", attack("bite", "Nasty bite") + attack("scratch", "Quick scratch"), "")
                + encounter("rats", "cellar", "rat") + "</enemies>";
        EncounterSetup setup = spawn(xml, "rats");

        assertEquals("bite", abilityOf(setup, setup.enemies().getFirst(), gunther()));
    }

    @Test void anAbilityThatAnnouncesItselfKeepsItsText()
    {
        String xml = "<enemies>" + enemy("rat", "Rat", "", attack("bite", "Nasty bite", " announcement=\"{user} bites!\"") + attack("scratch", "Quick scratch"), "")
                + encounter("rats", "cellar", "rat") + "</enemies>";

        Enemy rat = spawn(xml, "rats").enemies().getFirst();

        assertTrue(rat.getListOfAbility().get(0).getAnnouncement().isPresent());
        assertTrue(rat.getListOfAbility().get(1).getAnnouncement().isEmpty());
    }

    // ---------- The id and the name ----------

    @Test void aPatternFollowsTheIdAndNotTheName()
    {
        EncounterSetup setup = spawn(file("<pattern><rotation><use ability=\"bite\"/></rotation></pattern>", "rat"), "rats");

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
