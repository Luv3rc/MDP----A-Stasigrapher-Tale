package it.unicam.cs.mpgc.rpg130268.combat.damage;

// What the calculator decided. The engine applies amount() and the UI can show "Critical!"
public record DamageResult(int amount, boolean critical)
{
    public DamageResult
    {
        if (amount < 0)
            throw new IllegalArgumentException("amount must not be negative");
    }
}