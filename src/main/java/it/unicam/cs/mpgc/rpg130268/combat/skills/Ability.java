package it.unicam.cs.mpgc.rpg130268.combat.skills;
import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;

import java.util.List;

public interface Ability
{
    String getName();
    int getApCost();
    TargetType getTargetType();

    default boolean canPerform() { return true; }

    void perform(List<Fighter> targets);
}
