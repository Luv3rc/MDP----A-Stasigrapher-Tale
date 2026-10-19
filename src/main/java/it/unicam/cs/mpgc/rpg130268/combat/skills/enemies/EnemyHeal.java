package it.unicam.cs.mpgc.rpg130268.combat.skills.enemies;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.skills.CombatContext;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;

import java.util.List;

public class EnemyHeal extends EnemyAbility
{
    private final double fractionOfMaxHealth;

    public EnemyHeal(Enemy user, String name, TargetType targetType, double fractionOfMaxHealth)
    {
        super(user, name, targetType);

        if (targetType == TargetType.ONE_ENEMY || targetType == TargetType.ALL_ENEMIES)
            throw new IllegalArgumentException("a heal cannot target enemies");
        if (fractionOfMaxHealth <= 0 || fractionOfMaxHealth > 1)
            throw new IllegalArgumentException("fractionOfMaxHealth must be in (0, 1]");

        this.fractionOfMaxHealth = fractionOfMaxHealth;
    }

    // Heal on itself is only worth using when wounded
    @Override
    protected boolean meetsRequirements()
    {
        return getTargetType() != TargetType.SELF || user.getHp() < user.getStats().maxHealth();
    }

    @Override
    protected void applyEffect(List<Fighter> targets, CombatContext context)
    {
        for (Fighter target : targets)
        {
            int amount = Math.max(1, (int) (target.getStats().maxHealth() * fractionOfMaxHealth));
            context.heal(user, target, amount);
        }
    }
}