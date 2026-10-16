package it.unicam.cs.mpgc.rpg130268.combat;

public record Stats(int maxHealth, int strength, int defense, int agility, int luck)
{
    public Stats
    {
        if (maxHealth < 0) throw new IllegalArgumentException("maxHealth must be non-negative");
        if (strength < 0) throw new IllegalArgumentException("strength must be non-negative");
        if (defense < 0) throw new IllegalArgumentException("defense must be non-negative");
        if (agility < 0) throw new IllegalArgumentException("agility must be non-negative");
        if (luck < 0) throw new IllegalArgumentException("luck must be non-negative");
    }
}