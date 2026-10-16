package it.unicam.cs.mpgc.rpg130268.combat.skills;
import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;

public interface Ability
{
    String getName();
    int getApCost();

    void perform(Fighter user, Fighter target);
}
