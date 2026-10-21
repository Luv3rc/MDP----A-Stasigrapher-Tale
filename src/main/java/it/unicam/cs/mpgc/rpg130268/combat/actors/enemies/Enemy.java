package it.unicam.cs.mpgc.rpg130268.combat.actors.enemies;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAbility;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

// A Fighter that is not part of the party. It has no AP and what it can do is its list of abilities
// WHICH ability it uses on its turn (its attack pattern) will be a separate behavior,
// because choosing needs to know the state of the battle (who is alive, who is wounded, ...)
// The level tells the player how dangerous it is and the xp reward is what the party gets for defeating it.
// An enemy can wield any weapon (a goblin's club, a beast's claws...): the weapon decides how hard it hits
public class Enemy extends Fighter
{
    private final int level;
    private final int xpReward;
    private final List<EnemyAbility> listOfAbility = new ArrayList<>();

    public Enemy(String name, Stats stats)
    {
        this(name, stats, 1, 0);
    }

    public Enemy(String name, Stats stats, int level, int xpReward)
    {
        super(name, stats, EnumSet.allOf(WeaponType.class));

        if (level < 1)
            throw new IllegalArgumentException("level must be at least 1");

        if (xpReward < 0)
            throw new IllegalArgumentException("xpReward must not be negative");

        this.level = level;
        this.xpReward = xpReward;
    }

    public int getLevel() { return level; }
    public int getXpReward() { return xpReward; }

    public List<EnemyAbility> getListOfAbility() { return List.copyOf(listOfAbility); }

    // Same idea as Gunther.learn: the factory receives the enemy, because an ability needs to know its user
    // e.g. goblin.learn(e -> new EnemyAttack(e, "Club swing", TargetType.ONE_ENEMY, sequence))
    public void learn(Function<Enemy, EnemyAbility> factory)
    {
        Objects.requireNonNull(factory, "factory must not be null");

        EnemyAbility ability = Objects.requireNonNull(factory.apply(this), "The factory must not return null");

        boolean alreadyKnow = listOfAbility.stream()
                .anyMatch(a -> a.getName().equals(ability.getName()));

        if (!alreadyKnow) listOfAbility.add(ability);
    }
}
