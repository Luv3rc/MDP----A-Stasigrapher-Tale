package it.unicam.cs.mpgc.rpg130268.combat.damage;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;

public enum ScalingStat
{
    MAX_HEALTH, STRENGTH, DEFENSE, AGILITY, LUCK;

    public int valueIn(Stats stats)
    {
        return switch(this)
        {
            case MAX_HEALTH -> stats.maxHealth();
            case STRENGTH -> stats.strength();
            case DEFENSE -> stats.defense();
            case AGILITY -> stats.agility();
            case LUCK -> stats.luck();
        };
    }
}
