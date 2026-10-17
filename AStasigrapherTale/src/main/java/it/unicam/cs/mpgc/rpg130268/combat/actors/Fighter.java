package it.unicam.cs.mpgc.rpg130268.combat.actors;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;

// This is anyone who can take part in a combat. Knows nothing about AP
public class Fighter
{
    private final String name;
    private final Stats stats;
    private int hp;

    protected Fighter(String name, Stats stats)
    {
        this.name = name;
        this.stats = stats;
        this.hp = stats.maxHealth();
    }

    public String getName() { return name; }
    public Stats getStats() { return stats; }
    public int getHp() { return hp; }

    public boolean isAlive() { return hp > 0; }

    public void setHp(int hp)
    {
        if (hp < 0 || hp > stats.maxHealth())
            throw new IllegalArgumentException("HP must be between 0 and max health");

        this.hp = hp;
    }

    public void takeDamage(int damage)
    {
        if (damage < 0) throw new IllegalArgumentException("Damage must be non-negative");
        hp = Math.max(0, hp - damage);
    }

    public void heal(int heal)
    {
        if (heal < 0) throw new IllegalArgumentException("Heal must be non-negative");
        hp = Math.min(stats.maxHealth(), hp + heal);
    }

    // ? Will I need more here
    public void onBasicAttackLanded() {}
    public void onPerfectParry() {}
    public void onTurnStart() {}
    public void onBattleStart() {}

}
