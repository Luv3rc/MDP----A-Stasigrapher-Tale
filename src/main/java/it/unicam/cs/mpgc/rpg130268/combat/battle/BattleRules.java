package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;

import java.util.Objects;

/* *
    The rules of the battle engine that are not about damage numbers or reaction windows
    - Counter: when a fighter perfectly parries EVERY hit of an attack, it strikes back at the attacker
    - Dead zone: after every action (an attack, a heal...) nothing starts for this long, so what has just happened
      can be seen before the next thing begins. 0 = the next action starts as soon as the last one is over
    - Announcement: how long the text of an ability that announces itself ("Bandit heals!") stays on the screen.
      The ability begins only after it
* */
public record BattleRules(StrikeWeight counterWeight, long deadZoneMs, long announcementMs)
{
    public static final long DEFAULT_ANNOUNCEMENT_MS = 1500;

    public BattleRules
    {
        Objects.requireNonNull(counterWeight, "counterWeight must not be null");

        if (deadZoneMs < 0)
            throw new IllegalArgumentException("deadZoneMs must not be negative");
        if (announcementMs < 0)
            throw new IllegalArgumentException("announcementMs must not be negative");
    }

    // No dead zone, the usual time for the announcements
    public BattleRules(StrikeWeight counterWeight)
    {
        this(counterWeight, 0, DEFAULT_ANNOUNCEMENT_MS);
    }

    public static BattleRules defaults() { return new BattleRules(StrikeWeight.HEAVY); }
}