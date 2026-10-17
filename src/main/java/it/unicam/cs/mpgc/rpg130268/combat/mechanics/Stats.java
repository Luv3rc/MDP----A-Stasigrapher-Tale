package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

public record Stats(int maxHealth, int strength, int defense, int agility, int luck)
{
    public Stats
    {
        if (maxHealth <= 0) throw new IllegalArgumentException("maxHealth must be positive");
        if (strength < 0) throw new IllegalArgumentException("strength must be more than 0");
        if (defense < 0) throw new IllegalArgumentException("defense must be more than 0");
        if (agility < 0) throw new IllegalArgumentException("agility must be more than 0");
        if (luck < 0) throw new IllegalArgumentException("luck must be more than 0");
    }
}