package it.unicam.cs.mpgc.rpg130268.combat.battle;

public enum BattlePhase
{
    NOT_STARTED,
    WAITING_FOR_PLAYER,
    PLAYING,    // An action is being played on the clock -- whether from the player or an enemy
    OVER;
}
