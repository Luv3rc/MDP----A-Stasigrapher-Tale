package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurnManagerTest
{
    // Gunther has agility 6
    private Gunther gunther()
    {
        return new Gunther(new Stats(50, 8, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults());
    }

    private Enemy enemy(String name, int agility) { return new Enemy(name, new Stats(40, 5, 2, agility, 0)); }

    // The next n actors, by name
    private List<String> next(TurnManager turns, int n)
    {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < n; i++) names.add(turns.nextActor().orElseThrow().getName());
        return names;
    }

    // ---------- Order ----------

    @Test void theFastestActsFirstWhateverTheSide()
    {
        TurnManager turns = new TurnManager(List.of(gunther()), List.of(enemy("Imp", 9), enemy("Slug", 2)));

        assertEquals(List.of("Imp", "Gunther", "Slug"), next(turns, 3));
    }

    @Test void withTheSameAgilityThePartyActsFirst()
    {
        TurnManager turns = new TurnManager(List.of(gunther()), List.of(enemy("Goblin", 6)));

        assertEquals(List.of("Gunther", "Goblin"), next(turns, 2));
    }

    @Test void everyFighterActsOnceAndThenANewRoundStarts()
    {
        TurnManager turns = new TurnManager(List.of(gunther()), List.of(enemy("Imp", 9), enemy("Slug", 2)));

        assertEquals(0, turns.getRound());
        next(turns, 3);
        assertEquals(1, turns.getRound());

        assertEquals("Imp", turns.nextActor().orElseThrow().getName());   // the second round
        assertEquals(2, turns.getRound());
    }

    // ---------- Turn start ----------

    @Test void creatingTheManagerChangesNothing()
    {
        Gunther g = gunther();
        int ap = g.getAp().getCurrentAp();

        new TurnManager(List.of(g), List.of(enemy("Goblin", 3)));

        assertEquals(ap, g.getAp().getCurrentAp());
    }

    @Test void aFighterStartsItsTurnWhenItActsNotWhenTheRoundStarts()
    {
        Gunther g = gunther();                                   // agility 6: acts after the Imp
        int ap = g.getAp().getCurrentAp();
        TurnManager turns = new TurnManager(List.of(g), List.of(enemy("Imp", 9)));

        turns.nextActor();                                       // the Imp's turn
        assertEquals(ap, g.getAp().getCurrentAp());

        turns.nextActor();                                       // Gunther's turn
        assertEquals(ap + ApRules.defaults().apGainOnTurnStart(), g.getAp().getCurrentAp());
    }

    // ---------- Deaths ----------

    @Test void aFighterWhoDiedBeforeItsTurnIsSkipped()
    {
        Enemy imp = enemy("Imp", 9);
        Enemy slug = enemy("Slug", 2);
        TurnManager turns = new TurnManager(List.of(gunther()), List.of(imp, slug));

        turns.nextActor();                                       // the Imp acts...
        slug.setHp(0);                                           // ...and the Slug dies before its turn

        // Gunther acts, the dead Slug is skipped and the next round starts
        assertEquals(List.of("Gunther", "Imp"), next(turns, 2));
    }

    @Test void aFighterWhoIsDeadAtTheStartOfARoundIsNotInIt()
    {
        Enemy imp = enemy("Imp", 9);
        Enemy slug = enemy("Slug", 2);
        TurnManager turns = new TurnManager(List.of(gunther()), List.of(imp, slug));

        next(turns, 3);                                          // the whole first round
        imp.setHp(0);

        assertEquals(List.of("Gunther", "Slug"), next(turns, 2));
    }

    // ---------- The end of the battle ----------

    @Test void theOutcomeSaysWhoWon()
    {
        Gunther g = gunther();
        Enemy goblin = enemy("Goblin", 3);
        TurnManager turns = new TurnManager(List.of(g), List.of(goblin));

        assertEquals(BattleOutcome.ONGOING, turns.getBattleOutcome());
        assertFalse(turns.isBattleOver());

        goblin.setHp(0);
        assertEquals(BattleOutcome.VICTORY, turns.getBattleOutcome());
        assertTrue(turns.isBattleOver());
    }

    @Test void losingThePartyIsADefeat()
    {
        Gunther g = gunther();
        TurnManager turns = new TurnManager(List.of(g), List.of(enemy("Goblin", 3)));

        g.setHp(0);

        assertEquals(BattleOutcome.DEFEAT, turns.getBattleOutcome());
    }

    @Test void ifEverybodyFallsItIsADefeat()
    {
        Gunther g = gunther();
        Enemy goblin = enemy("Goblin", 3);
        TurnManager turns = new TurnManager(List.of(g), List.of(goblin));

        g.setHp(0);
        goblin.setHp(0);

        assertEquals(BattleOutcome.DEFEAT, turns.getBattleOutcome());
    }

    @Test void thereAreNoMoreActorsOnceTheBattleIsOverEvenInTheMiddleOfARound()
    {
        Gunther g = gunther();
        Enemy goblin = enemy("Goblin", 3);
        TurnManager turns = new TurnManager(List.of(g), List.of(goblin));

        turns.nextActor();                                       // Gunther acts, the goblin is still in the queue
        goblin.setHp(0);

        assertTrue(turns.nextActor().isEmpty());
    }

    @Test void whenEverybodyIsDeadThereIsNoActorAndNoError()
    {
        Gunther g = gunther();
        Enemy goblin = enemy("Goblin", 3);
        TurnManager turns = new TurnManager(List.of(g), List.of(goblin));

        g.setHp(0);
        goblin.setHp(0);

        assertTrue(turns.nextActor().isEmpty());
    }

    // ---------- Construction ----------

    @Test void theManagerKeepsItsOwnCopyOfTheLists()
    {
        List<Fighter> party = new ArrayList<>(List.of(gunther()));
        TurnManager turns = new TurnManager(party, List.of(enemy("Goblin", 3)));

        party.clear();

        assertFalse(turns.isBattleOver());
        assertEquals(List.of("Gunther", "Goblin"), next(turns, 2));
    }

    @Test void aBattleNeedsFightersOnBothSides()
    {
        assertThrows(IllegalArgumentException.class, () -> new TurnManager(List.of(), List.of(enemy("Goblin", 3))));
        assertThrows(IllegalArgumentException.class, () -> new TurnManager(List.of(gunther()), List.of()));
        assertThrows(NullPointerException.class, () -> new TurnManager(null, List.of(enemy("Goblin", 3))));
        assertThrows(NullPointerException.class, () -> new TurnManager(List.of(gunther()), null));
    }
}