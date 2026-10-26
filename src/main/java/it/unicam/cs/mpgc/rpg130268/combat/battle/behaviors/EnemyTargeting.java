package it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.EnemyAction;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAbility;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// What every behavior needs once it has decided WHICH ability to use; can it be used now, and on whom.
// One foe -> the first one; one ally -> the most wounded one. (The "who" can get smarter later, in one place)
public final class EnemyTargeting
{
    private EnemyTargeting() { }

    // Empty if the ability can't be used now, or if there is nobody to use it on
    public static Optional<EnemyAction> actionFor(Enemy self, EnemyAbility ability, List<Fighter> allies, List<Fighter> foes)
    {
        if (!ability.canPerform()) return Optional.empty();

        List<Fighter> targets = targetsFor(ability.getTargetType(), self, allies, foes);

        return targets.isEmpty() ? Optional.empty() : Optional.of(new EnemyAction(ability, targets));
    }

    // The ability of the enemy with this name, if it knows it
    public static Optional<EnemyAbility> abilityNamed(Enemy self, String name)
    {
        return self.getListOfAbility().stream().filter(ability -> ability.getName().equals(name)).findFirst();
    }

    private static List<Fighter> targetsFor(TargetType type, Enemy self, List<Fighter> allies, List<Fighter> foes)
    {
        return switch (type)
        {
            case SELF -> List.of(self);
            case ONE_ENEMY -> foes.isEmpty() ? List.of() : List.of(foes.getFirst());
            case ALL_ENEMIES -> foes;
            case ONE_ALLY -> allies.stream().min(Comparator.comparingDouble(EnemyTargeting::healthRatio)).map(List::of).orElse(List.of());
            case ALL_ALLIES -> allies;
        };
    }

    private static double healthRatio(Fighter fighter) { return (double) fighter.getHp() / fighter.getStats().maxHealth(); }
}