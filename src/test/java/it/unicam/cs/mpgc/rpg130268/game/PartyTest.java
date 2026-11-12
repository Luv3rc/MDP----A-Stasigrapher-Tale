package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilityCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.MemberCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.TestMembers;
import it.unicam.cs.mpgc.rpg130268.game.data.WeaponCatalog;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartyTest
{
    private static final WeaponCatalog WEAPONS = WeaponCatalog.fromResource(GameData.WEAPONS_FILE);

    private static final MemberCatalog CATALOG = MemberCatalog.fromResource(GameData.MEMBERS_FILE, WEAPONS, PartyAbilityCatalog.standard());

    private static Party ofTheGame() { return new Party(CATALOG); }

    private static Party withTwoMembers()
    {
        return new Party(MemberCatalog.load(new ByteArrayInputStream(TestMembers.TWO_MEMBERS.getBytes(StandardCharsets.UTF_8)),
                "test.xml", WEAPONS, PartyAbilityCatalog.standard(), TestMembers.withSquire()));
    }

    @Test void thePartyStartsWithGuntherAtLevelOne()
    {
        Party party = ofTheGame();

        assertEquals(1, party.members().size());
        assertEquals("Gunther", party.protagonist().getName());
        assertEquals(CATALOG.startingState("gunther").level(), party.protagonist().getLevel());
        assertEquals(0, party.protagonist().getCurrentXp());
        assertSame(party.protagonist(), party.members().getFirst());
    }

    @Test void guntherStartsWithHisWeaponAndHisAbilities()
    {
        PartyMember gunther = ofTheGame().protagonist();
        MemberState start = CATALOG.startingState("gunther");

        assertEquals(WEAPONS.weapon(start.weaponId()).name(), gunther.getWeapon().name());
        assertEquals(start.abilityIds(), gunther.getListOfAbility().stream().map(Ability::getId).toList());
    }

    @Test void restoringMakesGuntherAgainAsHeWas()
    {
        Party party = ofTheGame();
        PartyMember old = party.protagonist();
        int level = old.getLevel();
        int unspent = old.getUnspentPoints();
        old.gainXp(old.getXpForNextLevel() + 50);                 // a level and 50 more
        old.takeDamage(30);
        List<MemberState> saved = party.state();

        party.reset();
        party.restore(saved);

        assertNotSame(old, party.protagonist());
        assertEquals(level + 1, party.protagonist().getLevel());
        assertEquals(50, party.protagonist().getCurrentXp());
        assertEquals(old.getHp(), party.protagonist().getHp());
        assertEquals(unspent + old.getStatRules().pointsPerLevel(), party.protagonist().getUnspentPoints());      // what he had and the points of the level
    }

    @Test void whatTheMemberCarriesAndDidIsNotLostWhenTheStateIsRestored()
    {
        Party party = ofTheGame();
        int strength = party.protagonist().getStats().strength();
        int unspent = party.protagonist().getUnspentPoints();
        String weapon = party.protagonist().getWeapon().name();
        List<String> abilities = party.protagonist().getListOfAbility().stream().map(Ability::getId).toList();
        party.protagonist().gainXp(party.protagonist().getXpForNextLevel());       // a level, so there are points to spend
        party.protagonist().spendPoints(ScalingStat.STRENGTH, 2);
        List<MemberState> saved = party.state();

        party.reset();
        party.restore(saved);

        assertEquals(strength + 2, party.protagonist().getStats().strength());
        assertEquals(unspent + party.protagonist().getStatRules().pointsPerLevel() - 2, party.protagonist().getUnspentPoints());
        assertEquals(weapon, party.protagonist().getWeapon().name());
        assertEquals(abilities, party.protagonist().getListOfAbility().stream().map(Ability::getId).toList());
    }

    @Test void resetGoesBackToTheStartOfTheGame()
    {
        Party party = ofTheGame();
        int level = party.protagonist().getLevel();
        party.protagonist().gainXp(250);

        party.reset();

        assertEquals(level, party.protagonist().getLevel());
        assertEquals(0, party.protagonist().getCurrentXp());
    }

    // ---------- More than one member ----------

    @Test void thePartyCanHaveManyMembersInOrder()
    {
        Party party = withTwoMembers();

        assertEquals(List.of("gunther", "squire"), party.members().stream().map(PartyMember::getId).toList());
        assertEquals("Gunther", party.protagonist().getName());
        assertEquals("Squire", party.member("squire").getName());
    }

    @Test void everyMemberComesBackAsHeWas()
    {
        Party party = withTwoMembers();
        party.member("squire").gainXp(300);
        party.member("gunther").setHp(50);
        List<MemberState> saved = party.state();

        party.reset();
        party.restore(saved);

        assertEquals(List.of("gunther", "squire"), party.members().stream().map(PartyMember::getId).toList());
        assertEquals(2, party.member("squire").getLevel());
        assertEquals(50, party.member("gunther").getHp());
    }

    @Test void aMemberThatIsNotInThePartyIsAnError()
    {
        assertThrows(IllegalArgumentException.class, () -> ofTheGame().member("squire"));
    }

    @Test void aStateListMustBeAPartyThatMakesSense()
    {
        Party party = ofTheGame();
        MemberState gunther = party.state().getFirst();

        assertThrows(IllegalArgumentException.class, () -> party.restore(List.of()));
        assertThrows(IllegalArgumentException.class, () -> party.restore(List.of(gunther, gunther)));
        assertThrows(NullPointerException.class, () -> party.restore(null));
    }
}
