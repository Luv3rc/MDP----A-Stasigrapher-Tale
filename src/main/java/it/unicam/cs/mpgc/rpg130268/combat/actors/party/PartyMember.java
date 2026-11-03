package it.unicam.cs.mpgc.rpg130268.combat.actors.party;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.LevelRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApPool;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;

import java.util.List;
import java.util.Objects;
import java.util.Set;

// This is anyone who is part of a party.
// So it's a Fighter with AP and a set of abilities and can be controlled by a player
// His stats are POINTS (see StatPoints): at every level he gets some to spend (at a campfire) and his StatRules
// say how the points become stats (the base health and the health per point change from member to member)
public abstract class PartyMember extends Fighter
{
    private int level;
    private int currentXp;
    private final StatRules statRules;
    private StatPoints statPoints;
    private int unspentPoints;
    private final LevelRules levelRules;
    private final ApPool ap;
    private final ApRules apRules;

    public abstract List<Ability> getListOfAbility();


    // A member with ready-made stats (they are his points, on top of a base health), and nothing to spend.
    // It is what the tests and the demo battles use
    public PartyMember
            (String name, Stats stats, Set<WeaponType> allowedWeapons, int maxAp, int startingAp, ApRules apRules,
             LevelRules levelRules, int startingLevel, int startingXp)
    {
        this(name, StatRules.around(stats), StatPoints.of(stats), 0, allowedWeapons, maxAp, startingAp, apRules,
                levelRules, startingLevel, startingXp);
    }

    // A member made from his rules and his points: the one of the game, and the one a save brings back
    public PartyMember
            (String name, StatRules statRules, StatPoints statPoints, int unspentPoints, Set<WeaponType> allowedWeapons,
             int maxAp, int startingAp, ApRules apRules, LevelRules levelRules, int startingLevel, int startingXp)
    {
        super(name, Objects.requireNonNull(statRules, "statRules must not be null")
                .statsFor(Objects.requireNonNull(statPoints, "statPoints must not be null")), allowedWeapons);

        if (unspentPoints < 0)
            throw new IllegalArgumentException("unspentPoints must not be negative");

        this.statRules = statRules;
        this.statPoints = statPoints;
        this.unspentPoints = unspentPoints;

        this.ap = new ApPool(maxAp, startingAp);
        this.apRules = apRules;

        this.levelRules = levelRules;

        if (startingLevel < 1 || startingLevel > levelRules.maxLevel())
            throw new IllegalArgumentException("Starting level must be between 1 and " + levelRules.maxLevel());

        if (startingXp < 0 || startingXp >= levelRules.requiredXpForLevel(startingLevel + 1))
            throw new IllegalArgumentException("Starting XP must be between 0 and " + (levelRules.requiredXpForLevel(startingLevel + 1) - 1));

        this.level = startingLevel;
        this.currentXp = startingXp;
    }

    public int getLevel () { return level; }
    public int getCurrentXp() { return currentXp; }
    public ApPool getAp() { return ap; }
    public StatRules getStatRules() { return statRules; }
    public StatPoints getStatPoints() { return statPoints; }
    public int getUnspentPoints() { return unspentPoints; }

    // Can a point go in this stat right now? (there is one to spend and the stat is not at its cap)
    public boolean canIncrease(ScalingStat stat)
    {
        return unspentPoints > 0 && statPoints.get(stat) < statRules.statCap();
    }

    // Puts points in a stat (the "Increase stats" of a campfire). The stats are recalculated at once;
    // if it is health, the max HP goes up (and the current HP with it, see Fighter.changeStats)
    public void spendPoints(ScalingStat stat, int amount)
    {
        Objects.requireNonNull(stat, "stat must not be null");

        if (amount < 1)
            throw new IllegalArgumentException("the points to spend must be at least 1");

        if (amount > unspentPoints)
            throw new IllegalStateException(getName() + " has only " + unspentPoints + " points to spend");

        if (statPoints.get(stat) + amount > statRules.statCap())
            throw new IllegalArgumentException(stat + " cannot go over " + statRules.statCap());

        statPoints = statPoints.plus(stat, amount);
        unspentPoints -= amount;
        changeStats(statRules.statsFor(statPoints));
    }

    public void gainXp(int xp)
    {
        if (xp < 0)
            throw new IllegalArgumentException("XP gained must be non-negative");

        if (level >= levelRules.maxLevel())
            return;

        currentXp += xp;

        // The player could gain enough XP to level up multiple times, so we need to check in a loop
        while (level < levelRules.maxLevel() && currentXp >= levelRules.requiredXpForLevel(level + 1))
        {
            currentXp -= levelRules.requiredXpForLevel(level + 1);
            level ++;
            unspentPoints += statRules.pointsPerLevel();      // the player decides where they go, at a campfire

            onLevelUp();
        }
    }

    // What a member does of his own when he levels up (the points to spend are already given). It can vary
    // depending on the party member, so it's abstract and must be implemented by subclasses
    protected abstract void onLevelUp();

    @Override
    public void onBasicAttackLanded() { ap.gainAp(apRules.apGainOnBasicAttack()); }
    @Override
    public void onPerfectParry() { ap.gainAp(apRules.apGainOnPerfectParry()); }
    @Override
    public void onTurnStart() { ap.gainAp(apRules.apGainOnTurnStart()); }
    @Override
    public void onBattleStart() { ap.resetToStartingValue(); }
}
