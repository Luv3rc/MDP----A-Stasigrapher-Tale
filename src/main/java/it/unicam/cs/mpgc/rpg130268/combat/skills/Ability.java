package it.unicam.cs.mpgc.rpg130268.combat.skills;
import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;

import java.util.List;
import java.util.Optional;

public interface Ability
{
    String getName();
    String getDescription();
    int getApCost();
    TargetType getTargetType();

    default boolean canPerform() { return true; }

    // The text that pops up before the ability starts ("Bandit heals!"). Not every ability has one
    default Optional<String> getAnnouncement() { return Optional.empty(); }

    // The ability only DESCRIBES what happens. The CombatContext (the engine) decides how:
    // it calculates and applies the damage, waits for reactions, reports events...
    void perform(List<Fighter> targets, CombatContext context);
}