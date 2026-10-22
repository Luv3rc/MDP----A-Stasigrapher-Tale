package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;

import java.util.List;
import java.util.Optional;

// How an enemy chooses what to do on its turn (its "attack pattern"). It is separate from the Enemy because
// choosing needs to know the state of the battle: who is alive and who is wounded.
// The allies and the foes are the LIVING fighters, from the point of view of the enemy
// (its allies include itself; its foes are the party)
public interface EnemyBehavior
{
    // Empty means the enemy will do nothing this turn (it will be skipped)
    Optional<EnemyAction> choose(Enemy self, List<Fighter> allies, List<Fighter> foes);
}
