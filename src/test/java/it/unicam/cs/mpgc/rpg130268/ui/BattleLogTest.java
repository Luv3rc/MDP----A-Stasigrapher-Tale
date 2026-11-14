package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BattleLogTest
{
    private final Enemy goblin = new Enemy("Goblin", new Stats(30, 5, 3, 3, 3));
    private final Enemy wolf = new Enemy("Wolf", new Stats(30, 5, 3, 3, 3));

    @Test void aTurnIsAnnounced()
    {
        assertEquals("-- Turn of Goblin --", BattleLog.textOf(new BattleEvent.TurnStarted(goblin, 0)));
    }

    @Test void healingAndDefeatAreTold()
    {
        assertEquals("Wolf recovers 7 HP.", BattleLog.textOf(new BattleEvent.Healed(goblin, wolf, 7, 0)));
        assertEquals("Wolf is defeated!", BattleLog.textOf(new BattleEvent.Defeated(wolf, 0)));
    }

    @Test void aCounterTellsIfItWasCritical()
    {
        assertEquals("Wolf counters Goblin for 9!", BattleLog.textOf(new BattleEvent.Countered(wolf, goblin, 9, false, 0)));
        assertEquals("Wolf counters Goblin for 9 (CRITICAL)!", BattleLog.textOf(new BattleEvent.Countered(wolf, goblin, 9, true, 0)));
    }

    @Test void anAttackNamesAllItsTargets()
    {
        BattleEvent.AttackStarted event = new BattleEvent.AttackStarted(goblin, List.of(wolf, goblin), List.of(), 0);

        assertEquals("Goblin attacks Wolf, Goblin!", BattleLog.textOf(event));
    }

    @Test void anEventCanBeSilentAndThenTheLogGetsNothing()
    {
        BattleLog log = new BattleLog();

        log.record(new BattleEvent.Swing(goblin, List.of(wolf), null, 0));
        assertTrue(log.lines().isEmpty());

        log.record(new BattleEvent.Defeated(wolf, 0));
        log.add("extra");
        assertEquals(List.of("Wolf is defeated!", "extra"), log.lines());
    }

    @Test void theLinesAreACopy()
    {
        BattleLog log = new BattleLog();
        log.add("a");

        assertThrows(UnsupportedOperationException.class, () -> log.lines().add("b"));
    }
}
