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

    private static Party ofTheGame()
    {
        return new Party(MemberCatalog.fromResource(GameData.MEMBERS_FILE, WEAPONS, PartyAbilityCatalog.standard()));
    }

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
        assertEquals(1, party.protagonist().getLevel());
        assertEquals(0, party.protagonist().getCurrentXp());
        assertSame(party.protagonist(), party.members().getFirst());
    }

    @Test void guntherStartsWithHisWeaponAndHisAbilities()
    {
        PartyMember gunther = ofTheGame().protagonist();

        assertEquals("Thesmosia", gunther.getWeapon().name());
        assertEquals(4, gunther.getListOfAbility().size());
    }

    @Test void restoringMakesGuntherAgainAsHeWas()
    {
        Party party = ofTheGame();
        PartyMember old = party.protagonist();
        old.gainXp(250);
        old.takeDamage(30);
        List<MemberState> saved = party.state();

        party.reset();
        party.restore(saved);

        assertNotSame(old, party.protagonist());
        assertEquals(2, party.protagonist().getLevel());
        assertEquals(50, party.protagonist().getCurrentXp());
        assertEquals(old.getHp(), party.protagonist().getHp());
        assertEquals(6, party.protagonist().getUnspentPoints());       // the 3 of the start and the 3 of the level
    }

    @Test void whatTheMemberCarriesAndDidIsNotLostWhenTheStateIsRestored()
    {
        Party party = ofTheGame();
        party.protagonist().gainXp(200);
        party.protagonist().spendPoints(ScalingStat.STRENGTH, 2);
        List<MemberState> saved = party.state();

        party.reset();
        party.restore(saved);

        assertEquals(2, party.protagonist().getStats().strength());
        assertEquals(4, party.protagonist().getUnspentPoints());       // 3 + 3 and two of them spent
        assertEquals("Thesmosia", party.protagonist().getWeapon().name());
        assertEquals(List.of("basic-attack", "cuts-for-ink", "stasigraphic-fury", "thirst-for-ink"),
                party.protagonist().getListOfAbility().stream().map(Ability::getId).toList());
    }

    @Test void resetGoesBackToTheStartOfTheGame()
    {
        Party party = ofTheGame();
        party.protagonist().gainXp(250);

        party.reset();

        assertEquals(1, party.protagonist().getLevel());
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
