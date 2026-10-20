package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;

import java.util.Objects;

// The rules of the battle engine that are not about damage numbers or reaction windows
// Counter: when a fighter perfectly parries EVERY hit of an attack, it strikes back at the attacker
public record BattleRules(StrikeWeight counterWeight)
{
    public BattleRules { Objects.requireNonNull(counterWeight, "counterWeight must not be null"); }

    public static BattleRules defaults() { return new BattleRules(StrikeWeight.HEAVY); }
}
