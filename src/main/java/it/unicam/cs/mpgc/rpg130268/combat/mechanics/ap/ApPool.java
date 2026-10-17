package it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap;

public class ApPool
{
    private final int maxAp;
    private int currentAp;
    private final int startingAp;

    public ApPool (int maxAp, int startingAp)
    {
        if (maxAp < 0)
                throw new IllegalArgumentException("maxAp must be greater than 0");

        if (startingAp < 0 || startingAp > maxAp)
                throw new IllegalArgumentException("startingAp must be between 0 and maxAp");

        this.maxAp = maxAp;
        this.startingAp = startingAp;
        this.currentAp = startingAp;
    }

    public int getMaxAp() { return maxAp; }
    public int getCurrentAp() { return currentAp; }

    public boolean canAffordAbility(int cost) { return currentAp >= cost; }

    public void spendAp(int cost)
    {
        if (!canAffordAbility(cost))
            throw new IllegalArgumentException("Not enough AP!");

        currentAp -= cost;
    }

    public void fillAp() { this.currentAp = this.maxAp; }

    public void gainAp(int amount)
    {
        // Math.max avoid that negative values subtract AP (bug/exploit)
        // Math.min block the total at the maximum cap (maxAp)
        currentAp = Math.min(maxAp, currentAp + Math.max(0, amount));
    }

    // Used at the start of the battle to set the AP to its starting value (usually 3)
    public void resetToStartingValue() { currentAp = startingAp; }
}
