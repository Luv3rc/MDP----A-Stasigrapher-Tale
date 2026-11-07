package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;

// How a battle that is part of the story leaves the screen. The BattleScreen tells what happened and the game decides
// what comes next (see GameApp)
interface BattleExit
{
    // The screen waits a moment first, to let the player see the end of the fight
    void onWon(BattleEvent.BattleEnded end);

    // The question about trying again is not asked here, the defeat has a screen of its own
    void onLost();
}
