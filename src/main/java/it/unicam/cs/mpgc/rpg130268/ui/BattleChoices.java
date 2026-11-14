package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;

import java.util.List;

public interface BattleChoices
{
    boolean isOver();

    // The player can choose now
    boolean isChoosing();

    // The player can choose, and has not picked an ability that is waiting for its target
    boolean isMenuOpen();

    // An ability has been chosen and it needs a target among getTargetCandidates()
    boolean isTargeting();

    List<Ability> getAbilities();

    List<Fighter> getTargetCandidates();

    void chooseAbility(Ability ability);

    void chooseTarget(Fighter target);

    void cancel();
}
