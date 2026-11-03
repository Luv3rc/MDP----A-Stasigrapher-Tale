package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;

import java.util.Objects;

/* *
    The points a party member has put in each stat. A stat IS its points (strength 12 = 12 points in strength),
    except health, whose points are turned into max HP by the StatRules of the member (base + points * HP per point).
    Enemies have no points and their stats are written as they are, in the XML.
* */
public record StatPoints(int health, int strength, int defense, int agility, int luck)
{
    public static final StatPoints NONE = new StatPoints(0, 0, 0, 0, 0);

    // The cap of a stat is not checked here: it can change from member to member (see StatRules)
    public StatPoints
    {
        if (health < 0) throw new IllegalArgumentException("health points must not be negative");
        if (strength < 0) throw new IllegalArgumentException("strength points must not be negative");
        if (defense < 0) throw new IllegalArgumentException("defense points must not be negative");
        if (agility < 0) throw new IllegalArgumentException("agility points must not be negative");
        if (luck < 0) throw new IllegalArgumentException("luck points must not be negative");
    }

    // The points that give exactly these stats on top of a base health
    // (it is how a member made from ready-made Stats fits the points system)
    public static StatPoints of(Stats stats)
    {
        Objects.requireNonNull(stats, "stats must not be null");

        return new StatPoints(0, stats.strength(), stats.defense(), stats.agility(), stats.luck());
    }

    // MAX_HEALTH is the stat of the health points
    public int get(ScalingStat stat)
    {
        Objects.requireNonNull(stat, "stat must not be null");

        return switch (stat)
        {
            case MAX_HEALTH -> health;
            case STRENGTH -> strength;
            case DEFENSE -> defense;
            case AGILITY -> agility;
            case LUCK -> luck;
        };
    }

    // A NEW StatPoints with some more points in one stat
    public StatPoints plus(ScalingStat stat, int amount)
    {
        Objects.requireNonNull(stat, "stat must not be null");

        if (amount < 1)
            throw new IllegalArgumentException("the points to add must be at least 1");

        return switch (stat)
        {
            case MAX_HEALTH -> new StatPoints(health + amount, strength, defense, agility, luck);
            case STRENGTH -> new StatPoints(health, strength + amount, defense, agility, luck);
            case DEFENSE -> new StatPoints(health, strength, defense + amount, agility, luck);
            case AGILITY -> new StatPoints(health, strength, defense, agility + amount, luck);
            case LUCK -> new StatPoints(health, strength, defense, agility, luck + amount);
        };
    }

    public int total() { return health + strength + defense + agility + luck; }
}
