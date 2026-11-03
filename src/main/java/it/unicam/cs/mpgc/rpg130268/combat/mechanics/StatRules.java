package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageRules;

import java.util.Objects;

/* *
    How the points of ONE party member become stats. Every member has his own
    - baseHealth: the max HP with no points in health (Gunther: 164)
    - healthPerPoint: the max HP that each point in health gives (Gunther: 15)
    - pointsPerLevel: the points the member gets to spend at every level (3)
    - statCap: the most points a stat can have (60)
    The other stats are just their points. The enemies don't use this
* */
public record StatRules(int baseHealth, int healthPerPoint, int pointsPerLevel, int statCap)
{
    public static final int POINTS_PER_LEVEL = 3;

    public StatRules
    {
        if (baseHealth <= 0)
            throw new IllegalArgumentException("baseHealth must be positive");
        if (healthPerPoint < 0)
            throw new IllegalArgumentException("healthPerPoint must not be negative");
        if (pointsPerLevel < 0)
            throw new IllegalArgumentException("pointsPerLevel must not be negative");
        if (statCap < 1)
            throw new IllegalArgumentException("statCap must be at least 1");
    }

    // The usual rules (3 points per level, the cap of the game) for a member with his own health
    public static StatRules of(int baseHealth, int healthPerPoint)
    {
        return new StatRules(baseHealth, healthPerPoint, POINTS_PER_LEVEL, DamageRules.STAT_CAP);
    }

    // The rules of a member made from ready-made Stats (see StatPoints.of)
    // its max HP is the base and it gets the usual 15 HP for every point put in health from then on
    public static StatRules around(Stats stats)
    {
        Objects.requireNonNull(stats, "stats must not be null");

        return of(stats.maxHealth(), 15);
    }

    public Stats statsFor(StatPoints points)
    {
        Objects.requireNonNull(points, "points must not be null");

        return new Stats(baseHealth + healthPerPoint * points.health(),
                points.strength(), points.defense(), points.agility(), points.luck());
    }
}
