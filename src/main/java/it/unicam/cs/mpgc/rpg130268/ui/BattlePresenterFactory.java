package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.game.BattleFactory;
import it.unicam.cs.mpgc.rpg130268.game.EncounterSetup;

import java.util.List;

// It makes the Battle, with the rules of the game, and the BattlePresenter around it,
// with the background of the place where it happens
public final class BattlePresenterFactory
{
    private static final long PENALTY_MS = 400;

    private BattlePresenterFactory() { }

    public static BattlePresenter create(List<PartyMember> party, EncounterSetup setup)
    {
        Battle battle = BattleFactory.create(party, setup);

        BattlePresenter presenter = new BattlePresenter(battle, party, setup.enemies(),
                BattleFactory.reactionWindows(), new InputLockout(PENALTY_MS));
        presenter.setArea(setup.area());

        return presenter;
    }
}
