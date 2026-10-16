package it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap;

// Those are the rules for how AP works in combat
public record ApRules(int apGainOnBasicAttack, int apGainOnPerfectParry)
{
    public static ApRules defaults() { return new ApRules(1, 1); }
}
