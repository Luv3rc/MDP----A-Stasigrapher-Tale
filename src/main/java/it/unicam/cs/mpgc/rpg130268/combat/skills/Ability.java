package it.unicam.cs.mpgc.rpg130268.combat.skills;
import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;

import java.util.List;

public interface Ability
{
    String getName();
    int getApCost();
    TargetType getTargetType();

    default boolean canPerform() { return true; }

    // The ability only DESCRIBES what happens. The CombatContext (the engine) decides how:
    // it calculates and applies the damage, waits for reactions, reports events...
    void perform(List<Fighter> targets, CombatContext context);
}
