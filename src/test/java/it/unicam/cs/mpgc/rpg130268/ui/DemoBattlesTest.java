package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The demo is made of the files of the game, so these tests ask the files what is in them. The numbers of the game
// (which are tuned all the time) are not written here
class DemoBattlesTest
{
    private static final GameData DATA = GameData.bundled();

    // ---------- Which encounter ----------

    @Test void withNoArgumentsTheDemoIsTheDefaultEncounterAndItExists()
    {
        assertEquals(DemoBattles.DEFAULT_ENCOUNTER, DemoBattles.encounterFrom(List.of()));
        assertTrue(DATA.encounters().contains(DemoBattles.DEFAULT_ENCOUNTER));
    }

    @Test void theEncounterCanComeAfterTheBattleFlag()
    {
        assertEquals("goblin", DemoBattles.encounterFrom(List.of("--battle", "goblin")));
        assertEquals("bandit", DemoBattles.encounterFrom(List.of("--other", "--battle", "bandit")));
    }

    @Test void withTheFlagAloneOrFollowedByAnotherFlagTheDefaultIsUsed()
    {
        assertEquals(DemoBattles.DEFAULT_ENCOUNTER, DemoBattles.encounterFrom(List.of("--battle")));
        assertEquals(DemoBattles.DEFAULT_ENCOUNTER, DemoBattles.encounterFrom(List.of("--battle", "--verbose")));
    }

    @Test void withoutTheFlagTheFirstArgumentIsTheEncounter()
    {
        assertEquals("goblin-and-bandit", DemoBattles.encounterFrom(List.of("goblin-and-bandit")));
    }

    // ---------- The data of the game ----------

    @Test void theDemoPartyIsTheOneOfANewGameAsMembersXmlSays()
    {
        List<PartyMember> party = DemoBattles.newParty();

        assertEquals(DATA.members().startingParty(), party.stream().map(PartyMember::getId).toList());

        for (PartyMember member : party)
            assertEquals(DATA.members().startingState(member.getId()), it.unicam.cs.mpgc.rpg130268.game.MemberState.of(member));
    }

    @Test void theDemoEnemiesAreTheOnesOfEnemiesXml()
    {
        BattlePresenter presenter = DemoBattles.create();

        Enemy fromTheFile = DATA.encounters().encounter(DemoBattles.DEFAULT_ENCOUNTER).spawn().enemies().getFirst();
        FighterView shown = presenter.getEnemies().getFirst();

        assertEquals(fromTheFile.getName(), shown.getName());
        assertEquals(fromTheFile.getArtName(), shown.getArtName());
        assertEquals(fromTheFile.getStats().maxHealth(), shown.getMaxHp());
    }

    @Test void everyEncounterOfTheFileCanBeTriedInTheDemo()
    {
        for (String id : DATA.encounters().ids())
        {
            BattlePresenter presenter = DemoBattles.create(id);
            presenter.start();

            assertFalse(presenter.getEnemies().isEmpty(), id);
        }
    }

    @Test void everyCallMakesEverythingNew()
    {
        String any = DATA.encounters().ids().iterator().next();
        PartyMember first = DemoBattles.newParty().getFirst();
        PartyMember second = DemoBattles.newParty().getFirst();

        assertNotSame(first, second);
        assertNotSame(DemoBattles.encounter(any).enemies().getFirst(), DemoBattles.encounter(any).enemies().getFirst());
    }

    @Test void anEncounterThatDoesNotExistSaysWhichOnesDo()
    {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> DemoBattles.create("no-such-encounter"));

        assertTrue(error.getMessage().contains("no-such-encounter"));

        for (String id : DATA.encounters().ids())
            assertTrue(error.getMessage().contains(id), id);
    }
}
