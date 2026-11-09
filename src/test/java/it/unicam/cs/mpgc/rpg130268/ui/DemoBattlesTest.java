package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DemoBattlesTest
{
    // ---------- Which encounter ----------

    @Test void withNoArgumentsTheDemoIsTheTutorial()
    {
        assertEquals("sariel", DemoBattles.encounterFrom(List.of()));
        assertEquals("sariel", DemoBattles.DEFAULT_ENCOUNTER);
    }

    @Test void theEncounterCanComeAfterTheBattleFlag()
    {
        assertEquals("goblin", DemoBattles.encounterFrom(List.of("--battle", "goblin")));
        assertEquals("bandit", DemoBattles.encounterFrom(List.of("--other", "--battle", "bandit")));
    }

    @Test void withTheFlagAloneOrFollowedByAnotherFlagTheDefaultIsUsed()
    {
        assertEquals("sariel", DemoBattles.encounterFrom(List.of("--battle")));
        assertEquals("sariel", DemoBattles.encounterFrom(List.of("--battle", "--verbose")));
    }

    @Test void withoutTheFlagTheFirstArgumentIsTheEncounter()
    {
        assertEquals("goblin-and-bandit", DemoBattles.encounterFrom(List.of("goblin-and-bandit")));
    }

    // ---------- The data of the game ----------

    @Test void theDemoPartyIsTheOneOfANewGameAsMembersXmlSays()
    {
        List<PartyMember> party = DemoBattles.newParty();

        assertEquals(1, party.size());
        assertEquals("Gunther", party.getFirst().getName());
        assertEquals(new Stats(164, 0, 0, 0, 0), party.getFirst().getStats());
        assertEquals("Thesmosia", party.getFirst().getWeapon().name());
    }

    @Test void theDemoEnemiesAreTheOnesOfEnemiesXml()
    {
        BattlePresenter presenter = DemoBattles.create();

        BattlePresenter.FighterView sariel = presenter.getEnemies().getFirst();

        assertEquals("???", sariel.getName());
        assertEquals("sariel", sariel.getArtName());
        assertEquals(260, sariel.getMaxHp());
    }

    @Test void everyEncounterOfTheFileCanBeTriedInTheDemo()
    {
        for (String id : List.of("sariel", "goblin", "bandit", "goblin-and-bandit", "mattias-and-alex"))
        {
            BattlePresenter presenter = DemoBattles.create(id);
            presenter.start();

            assertFalse(presenter.getEnemies().isEmpty(), id);
        }
    }

    @Test void everyCallMakesEverythingNew()
    {
        PartyMember first = DemoBattles.newParty().getFirst();
        PartyMember second = DemoBattles.newParty().getFirst();

        assertNotSame(first, second);
        assertNotSame(DemoBattles.encounter("goblin").enemies().getFirst(), DemoBattles.encounter("goblin").enemies().getFirst());
    }

    @Test void anEncounterThatDoesNotExistSaysWhichOnesDo()
    {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> DemoBattles.create("dragon"));

        assertTrue(error.getMessage().contains("dragon"));
        assertTrue(error.getMessage().contains("sariel"));
        assertTrue(error.getMessage().contains("goblin-and-bandit"));
    }
}
