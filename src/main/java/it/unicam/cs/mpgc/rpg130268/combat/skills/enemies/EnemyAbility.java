package it.unicam.cs.mpgc.rpg130268.combat.skills.enemies;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.skills.AbstractAbility;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;

// Base class of everything an enemy can do (attacks, heals...). Enemies don't use AP
public abstract class EnemyAbility extends AbstractAbility<Enemy>
{
    protected EnemyAbility(Enemy user, String name, TargetType targetType)
    {
        super(user, name, targetType);
    }

    // So it can be chained: e.learn(e -> new EnemyHeal(...).withAnnouncement("{user} heals!"))
    @Override
    public EnemyAbility withAnnouncement(String text)
    {
        super.withAnnouncement(text);
        return this;
    }

    // Enemies don't use AP, so there is nothing to afford and nothing to pay
    @Override
    public final int getApCost() { return 0; }

    @Override
    protected boolean canAfford() { return true; }
    public String getDescription() { return ""; }

    @Override
    protected void payCost() {}
}