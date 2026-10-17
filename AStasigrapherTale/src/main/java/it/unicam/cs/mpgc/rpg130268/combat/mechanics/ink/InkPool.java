package it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink;

public class InkPool
{
    private int maxInk; // 10 at start, max of 20 based on level
    private int currentInk;

    public InkPool(int maxInk, int currentInk, InkRules rules)
    {
        if (maxInk > rules.maxCapacity())
            throw new IllegalArgumentException("maxInk must be less than or equal to the absolute max capacity");

        if (maxInk < 0)
            throw new IllegalArgumentException("maxInk must be non-negative");

        if (currentInk < 0 || currentInk > maxInk)
            throw new IllegalArgumentException("currentInk must be between 0 and maxInk");

        this.maxInk = maxInk;
        this.currentInk = currentInk;
    }

    public int getMaxInk() { return maxInk; }
    public int getCurrentInk() { return currentInk; }

    public boolean canAffordInk(int cost) { return currentInk >= cost; }

    public void spendInk(int cost)
    {
        if (cost < 0)
            throw new IllegalArgumentException("cost must be non-negative");

        if (!canAffordInk(cost))
            throw new IllegalArgumentException("cost must be greater than maxInk");

        currentInk -= cost;
    }

    public void fillInk() { this.currentInk = this.maxInk; }

    public void gainInk(int amount)
    {
        currentInk = Math.min(maxInk, currentInk + Math.max(0, amount));
    }

    // This method is used to increase the max capacity of the InkPool.
    // It's used when Gunther -- so the player -- levels up
    public void upgradeMaxCapacity(InkRules rules)
    {
        this.maxInk = Math.min(rules.maxCapacity(), this.maxInk + rules.capacityUpgradePerTenLevel());
    }

    // Drains ink and return it to the caller.
    // This is used when Gunther uses abilities which consumes ink to deal damage.
    public int drainInk()
    {
        int drainedInk = currentInk;
        currentInk = 0;
        return drainedInk;
    }

    // At the start of each battle, the InkPool (Gunther's Gauge) is reset to 0
    public void resetInk() { this.currentInk = 0; }

}
