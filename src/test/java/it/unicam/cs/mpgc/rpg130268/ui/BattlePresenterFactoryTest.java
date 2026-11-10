package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilities;
import it.unicam.cs.mpgc.rpg130268.game.EncounterSetup;
import it.unicam.cs.mpgc.rpg130268.game.data.EncounterCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.MemberCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.WeaponCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattlePresenterFactoryTest
{
    private static final MemberCatalog MEMBERS = MemberCatalog.fromResource(GameData.MEMBERS_FILE,
            WeaponCatalog.fromResource(GameData.WEAPONS_FILE), PartyAbilities.standard());
    private static final EncounterCatalog ENCOUNTERS = EncounterCatalog.fromResource(GameData.ENEMIES_FILE);

    private static BattlePresenter presenterFor(String encounter)
    {
        List<PartyMember> party = List.of(MEMBERS.newMember("gunther"));
        EncounterSetup setup = ENCOUNTERS.encounter(encounter).spawn();

        return BattlePresenterFactory.create(party, setup);
    }

    @Test void theBattleIsMadeFromTheEncounterAndItsAreaIsTheBackground()
    {
        BattlePresenter presenter = presenterFor("goblin");

        assertEquals("forest", presenter.getArea());
        assertEquals(1, presenter.getParty().size());
        assertEquals(1, presenter.getEnemies().size());
    }

    @Test void theBattleCanBeStarted()
    {
        BattlePresenter presenter = presenterFor("goblin-and-bandit");

        presenter.start();

        assertEquals(2, presenter.getEnemies().size());
        assertFalse(presenter.isOver());
    }

    @Test void anEnemyShownWithAnotherNameKeepsItsOwnPictures()
    {
        BattlePresenter.FighterView sariel = presenterFor("sariel").getEnemies().getFirst();

        assertEquals("???", sariel.getName());
        assertEquals("sariel", sariel.getArtName());
    }

    // Plays until it is the turn of the player (the "???" attacks first, because she is faster)
    private static BattlePresenter untilThePlayerChooses()
    {
        BattlePresenter presenter = presenterFor("sariel");
        presenter.setAlwaysAskTarget(true);
        presenter.start();

        for (int i = 0; i < 400 && !presenter.isChoosing() && !presenter.isOver(); i++)
            presenter.advance(50);

        return presenter;
    }

    @Test void theMenuIsOpenOnTheTurnOfThePlayer()
    {
        BattlePresenter presenter = untilThePlayerChooses();

        assertTrue(presenter.isChoosing());
        assertTrue(presenter.isMenuOpen());
    }

    @Test void theMenuGoesAwayWhileTheTargetIsChosenAndComesBackIfTheChoiceIsCancelled()
    {
        BattlePresenter presenter = untilThePlayerChooses();
        Ability basic = presenter.getAbilities().stream().filter(a -> a.getId().equals("basic-attack")).findFirst().orElseThrow();

        presenter.chooseAbility(basic);

        assertTrue(presenter.isTargeting());
        assertTrue(presenter.isChoosing());               // it is still his turn...
        assertFalse(presenter.isMenuOpen());              // ...but the menu is not there, he is choosing the target

        presenter.cancel();

        assertFalse(presenter.isTargeting());
        assertTrue(presenter.isMenuOpen());
    }

    @Test void theMenuIsNotOpenWhenItIsNotTheTurnOfThePlayer()
    {
        BattlePresenter presenter = presenterFor("sariel");
        presenter.start();                                // the "???" is faster, so the first turn is hers

        assertFalse(presenter.isMenuOpen());
    }

    @Test void theArtNameOfAPartyMemberIsHisName()
    {
        assertEquals("Gunther", presenterFor("goblin").getParty().getFirst().getArtName());
    }
}
