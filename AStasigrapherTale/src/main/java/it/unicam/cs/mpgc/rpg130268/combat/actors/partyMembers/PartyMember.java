package it.unicam.cs.mpgc.rpg130268.combat.actors.partyMembers;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.LevelRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApPool;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;

// This is anyone who is part of a party.
// So it's a Fighter with AP and a set of abilities and can be controlled by a player
public abstract class PartyMember extends Fighter
{
    private int level;
    private int currentXp;
    private final LevelRules levelRules;
    private final ApPool ap;
    private final ApRules apRules;

    public PartyMember(String name, Stats stats, int maxAp, int startingAp, ApRules apRules, LevelRules levelRules, int startingLevel, int startingXp)
    {
        super(name, stats);
        this.ap = new ApPool(maxAp, startingAp);
        this.apRules = apRules;

        this.levelRules = levelRules;

        if (startingLevel < 1 || startingLevel > levelRules.maxLevel())
            throw new IllegalArgumentException("Starting level must be between 1 and " + levelRules.maxLevel());

        if (startingXp < levelRules.requiredXpForLevel(startingLevel))
            throw new IllegalArgumentException("startingXp is too low for the provided startingLevel");

        this.level = startingLevel;
        this.currentXp = startingXp;
    }

    public int getLevel () { return level; }
    public int getCurrentXp() { return currentXp; }
    public ApPool ap() { return ap; }

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

            onLevelUp();
        }
    }

    // This method can vary depending on the party member, so it's abstract and must be implemented by subclasses
    protected abstract void onLevelUp();


    @Override
    public void onBasicAttackLanded() { ap.gainAp(apRules.apGainOnBasicAttack()); }
    @Override
    public void onPerfectParry() { ap.gainAp(apRules.apGainOnPerfectParry()); }
}
