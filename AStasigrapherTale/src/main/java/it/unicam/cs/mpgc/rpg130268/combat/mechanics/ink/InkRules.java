package it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink;

// Those are the rules for how Ink works in combat
public record InkRules(int maxCapacity, int inkGainPerHit, int capacityUpgradePerTenLevel)
{
    public static InkRules defaults() { return new InkRules(20, 1, 2); }
}