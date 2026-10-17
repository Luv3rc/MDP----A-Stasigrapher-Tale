package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

public record LevelRules(int maxLevel)
{
    public static LevelRules defaults() { return new LevelRules(60); }

    public int requiredXpForLevel(int level) { return level * 100; }

}
