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
    DamageResult resolveHit(Fighter attacker, Fighter target, Hit hit, double bonusMultiplier);

    default DamageResult resolveHit(Fighter attacker, Fighter target, Hit hit)
    {
        return resolveHit(attacker, target, hit, 1.0);
    }

    /* *
        A whole attack the target may react to, hit by hit (the Hit says how long and what the target can do).
        It stops as soon as the target is dead. Returns the total damage dealt.
        It takes the whole sequence because reactions are about the attack as a whole
        (e.g. parrying every hit gives a counter)
    */
    int resolveAttack(Fighter attacker, Fighter target, List<Hit> hits);

    // Restores health: never above the maximum, never reviving a dead fighter. Returns the health restored
    int heal(Fighter source, Fighter target, int amount);
}