package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageResult;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;

import java.util.List;

// What an ability can ask the engine to do. Abilities never calculate damage, apply it, print or wait:
// they say WHAT happens and the engine (BattleContext) decides HOW
public interface CombatContext
{
    /* *
        One hit nobody reacts to (the player's own strikes). It takes the whole Hit, not only its weight,
        because the Hit also says WHEN it lands (windUpMs): the engine can record it, so the UI can replay
        the combo over time while the domain resolves it at once
        The engine calculates the damage, applies it to the target and returns the result,
        so the ability can see the amount and whether it was a crit
        bonusMultiplier is for dynamic effects (e.g. the ink of Stasigraphic Fury)
    */
    // Symmetrical AoE hit for the player
    List<DamageResult> resolveHit(Fighter attacker, List<Fighter> targets, Hit hit, double bonusMultiplier);

    // Default wrappers so your single-target skills still compile perfectly
    default DamageResult resolveHit(Fighter attacker, Fighter target, Hit hit, double bonusMultiplier) {
        return resolveHit(attacker, List.of(target), hit, bonusMultiplier).getFirst();
    }

    default DamageResult resolveHit(Fighter attacker, Fighter target, Hit hit)
    {
        return resolveHit(attacker, target, hit, 1.0);
    }

    default List<DamageResult> resolveHit(Fighter attacker, List<Fighter> targets, Hit hit)
    {
        return resolveHit(attacker, targets, hit, 1.0);
    }

    /* *
        A whole attack the targets may react to, hit by hit (the Hit says how long and what they can do).
        A hit eats EVERYONE it is aimed at: there is one reaction per hit, and it counts for all the targets,
        so if it fails everyone takes the damage and if it is a perfect parry everyone parries.
        (An attack that hits different people differently is just several single target attacks)
        It stops when the attacker is dead or all the targets are
        It takes the whole sequence because reactions are about the attack as a whole
        (e.g. parrying every hit gives a counter)
    */
    void resolveAttack(Fighter attacker, List<Fighter> targets, List<Hit> hits);

    // Restores health: never above the maximum, never reviving a dead fighter. Returns the health restored
    int heal(Fighter source, Fighter target, int amount);
}