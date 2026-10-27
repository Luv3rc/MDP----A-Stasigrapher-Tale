package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOutcome;

import java.util.List;

// What happened in a battle, told to whoever shows it (the UI). The engine has already resolved all of it,
// the events only say what happened and WHEN, so the UI can replay it with "animations"
public sealed interface BattleEvent
{
    // Milliseconds since the start of the action (the clock restarts every time the events are drained)
    long atMs();

    // A live attack (see LiveBattleContext) begins
    // The hit number n lands at atMs + the wind-ups of the hits 0...n
    record AttackStarted(Fighter attacker, List<Fighter> targets, List<Hit> hits, long atMs) implements BattleEvent {}

    // A hit reached its target. damage is 0 if the target avoided it (see reaction)
    record HitResolved(Fighter attacker, Fighter target, ReactionOutcome reaction,
                       int damage, boolean critical, long atMs) implements BattleEvent {}

    // An ability that announces itself (e.g. "Bandit heals!"): the UI shows the text for durationMs. Whatever the
    // ability does begins after the text is gone
    record Announced(Fighter actor, String text, long durationMs, long atMs) implements BattleEvent {}

    // The turn of a fighter begins; a party member waits for the choice of the player, an enemy acts at once
    record TurnStarted(Fighter actor, long atMs) implements BattleEvent {}

    // The battle is over. xp is what every party member who is still standing received (0 if the party lost)
    record BattleEnded(BattleOutcome outcome, int xp, long atMs) implements BattleEvent {}

    // After parrying every hit of an attack, the defender strikes back
    record Countered(Fighter defender, Fighter attacker, int damage, boolean critical, long atMs) implements BattleEvent {}

    record Healed(Fighter source, Fighter target, int amount, long atMs) implements BattleEvent {}

    record Defeated(Fighter fighter, long atMs) implements BattleEvent {}
}