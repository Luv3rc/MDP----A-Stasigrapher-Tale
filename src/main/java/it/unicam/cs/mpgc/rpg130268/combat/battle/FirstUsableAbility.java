package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAbility;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// ! The simplest behavior -- That's why it has to be changed in the future for a better one
// The first ability the enemy can use (in the order it learned them) that has
// somebody to use it on. A real attack pattern (phases, "heal when wounded", weights...) will replace it
// Targets: one foe -> the first one; one ally -> the most wounded one
public class FirstUsableAbility implements EnemyBehavior
{
    @Override
    public Optional<EnemyAction> choose(Enemy self, List<Fighter> allies, List<Fighter> foes)
    {
        for (EnemyAbility ability : self.getListOfAbility())
        {
            if (!ability.canPerform()) continue;

            List<Fighter> targets = targetsFor(ability.getTargetType(), self, allies, foes);
            if (!targets.isEmpty())
                return Optional.of(new EnemyAction(ability, targets));
        }

        return Optional.empty();
    }

    private List<Fighter> targetsFor(TargetType type, Enemy self, List<Fighter> allies, List<Fighter> foes)
    {
        return switch (type)
        {
            case SELF -> List.of(self);
            case ONE_ENEMY -> foes.isEmpty() ? List.of() : List.of(foes.getFirst());
            case ALL_ENEMIES -> foes;
            case ONE_ALLY -> allies.stream().min(Comparator.comparingDouble(this::healthRatio)).map(List::of).orElse(List.of());
            case ALL_ALLIES -> allies;
        };
    }

    private double healthRatio(Fighter fighter) { return (double) fighter.getHp() / fighter.getStats().maxHealth(); }
}