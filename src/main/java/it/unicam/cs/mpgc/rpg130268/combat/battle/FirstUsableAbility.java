package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.EnemyBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.EnemyTargeting;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAbility;

import java.util.List;
import java.util.Optional;

// The simplest behavior: the first ability the enemy can use (in the order it learned them) that has
// somebody to use it on. It is the default for the enemies that have no pattern of their own
public class FirstUsableAbility implements EnemyBehavior
{
    @Override
    public Optional<EnemyAction> choose(Enemy self, List<Fighter> allies, List<Fighter> foes)
    {
        for (EnemyAbility ability : self.getListOfAbility())
        {
            Optional<EnemyAction> action = EnemyTargeting.actionFor(self, ability, allies, foes);

            if (action.isPresent()) return action;
        }

        return Optional.empty();
    }
}