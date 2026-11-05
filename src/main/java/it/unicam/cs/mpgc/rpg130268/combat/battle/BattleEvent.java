package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOutcome;

import java.util.List;
import java.util.Map;

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

    // A fighter begins a strike that cannot be reacted to (what the party does): it lands hit.windUpMs() after atMs,
    // and then comes the HitResolved. The UI uses it to show the strike coming (the animation, the cut of the blade)
    record Swing(Fighter attacker, List<Fighter> targets, Hit hit, long atMs) implements BattleEvent {}

    // An ability that announces itself (e.g. "Bandit heals!"): the UI shows the text for durationMs. Whatever the
    // ability does begins after the text is gone
    record Announced(Fighter actor, String text, long durationMs, long atMs) implements BattleEvent {}

    // The turn of a fighter begins; a party member waits for the choice of the player, an enemy acts at once
    record TurnStarted(Fighter actor, long atMs) implements BattleEvent {}

    // The battle is over. xpByMember is what every member of the party received (empty if the party lost); the
    // members who had fallen come back with 1 HP. totalXp() is what the party got in all
    record BattleEnded(BattleOutcome outcome, Map<PartyMember, Integer> xpByMember, long atMs) implements BattleEvent
    {
        public BattleEnded
        {
            xpByMember = Map.copyOf(xpByMember);
        }

        public int totalXp() { return xpByMember.values().stream().mapToInt(Integer::intValue).sum(); }
    }

    // After parrying every hit of an attack, the defender strikes back
    record Countered(Fighter defender, Fighter attacker, int damage, boolean critical, long atMs) implements BattleEvent {}

    record Healed(Fighter source, Fighter target, int amount, long atMs) implements BattleEvent {}

    record Defeated(Fighter fighter, long atMs) implements BattleEvent {}
}