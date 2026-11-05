package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattlePhase;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilities;
import it.unicam.cs.mpgc.rpg130268.game.data.EncounterCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.MemberCatalog;
import it.unicam.cs.mpgc.rpg130268.game.data.WeaponCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleFactoryTest
{
    private static List<PartyMember> party()
    {
        MemberCatalog members = MemberCatalog.fromResource(GameData.MEMBERS_FILE,
                WeaponCatalog.fromResource(GameData.WEAPONS_FILE), PartyAbilities.standard());

        return new Party(members).members();
    }

    @Test void aBattleMadeFromAnEncounterCanBeStarted()
    {
        EncounterSetup setup = EncounterCatalog.fromResource(GameData.ENEMIES_FILE).encounter("sariel").spawn();

        Battle battle = BattleFactory.create(party(), setup);

        assertEquals(BattlePhase.NOT_STARTED, battle.getPhase());
        battle.start();
        assertNotEquals(BattlePhase.NOT_STARTED, battle.getPhase());
    }

    @Test void everyBattleHasTheRulesOfTheGame()
    {
        assertTrue(BattleFactory.rules().deadZoneMs() > 0);
        assertTrue(BattleFactory.rules().announcementMs() > 0);
        assertNotNull(BattleFactory.reactionWindows());
        assertEquals(0.20, BattleFactory.xpRules().fallenPenalty());
    }
}
