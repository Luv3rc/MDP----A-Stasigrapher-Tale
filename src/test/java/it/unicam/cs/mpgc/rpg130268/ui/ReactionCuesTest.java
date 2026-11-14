package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReactionCuesTest
{
    // parry: 120 ms early, 40 late; dodge: 150 early, 50 late
    private final ReactionCues cues = new ReactionCues(new ReactionWindows(160, 200));
    private final Enemy goblin = new Enemy("Goblin", new Stats(30, 5, 3, 3, 3));

    private BattleEvent.AttackStarted attack(long atMs, Hit... hits)
    {
        return new BattleEvent.AttackStarted(goblin, List.of(goblin), List.of(hits), atMs);
    }

    private Hit hit(long windUpMs, ReactionOptions options) { return new Hit(windUpMs, options, StrikeWeight.NORMAL); }

    @Test void thereIsNoCueBeforeTheAttackStarts()
    {
        cues.remember(attack(500, hit(1000, ReactionOptions.PARRY_ONLY)));

        assertTrue(cues.cueAt(400).isEmpty());
    }

    @Test void theCueSaysWhereTheWindowsOfTheNextHitAre()
    {
        cues.remember(attack(500, hit(1000, ReactionOptions.PARRY_ONLY)));

        Cue cue = cues.cueAt(600).orElseThrow();

        assertEquals(500, cue.startMs());
        assertEquals(1500, cue.impactMs());
        assertTrue(cue.parry());
        assertEquals(1380, cue.parryFromMs());
        assertEquals(1540, cue.parryToMs());
        assertFalse(cue.dodge());
    }

    @Test void theHitsOfACombosComeOneAfterTheOther()
    {
        cues.remember(attack(0, hit(1000, ReactionOptions.PARRY_OR_DODGE), hit(500, ReactionOptions.DODGE_ONLY)));

        assertEquals(1000, cues.cueAt(10).orElseThrow().impactMs());

        cues.forgetClosed(1100);

        Cue second = cues.cueAt(1100).orElseThrow();
        assertEquals(1000, second.startMs());
        assertEquals(1500, second.impactMs());
        assertTrue(second.dodge());
        assertFalse(second.parry());
    }

    @Test void aHitThatCannotBeReactedToHasNoCue()
    {
        cues.remember(attack(0, hit(1000, ReactionOptions.NONE)));

        assertTrue(cues.cueAt(500).isEmpty());
    }

    @Test void aHitIsForgottenOnlyWhenItsWindowsAreClosed()
    {
        cues.remember(attack(0, hit(1000, ReactionOptions.PARRY_ONLY)));

        cues.forgetClosed(1040);                        // the late window is still open
        assertTrue(cues.cueAt(1040).isPresent());

        cues.forgetClosed(1041);
        assertTrue(cues.cueAt(1041).isEmpty());
    }
}
