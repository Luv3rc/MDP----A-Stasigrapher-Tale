package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.EnemyBehavior;

import java.util.List;
import java.util.Objects;

// Everything the UI needs to build a Battle against an encounter.
// The enemies, what they do and the name of the place, which is the background
// to show (see BattlePresenter.setArea)
public record EncounterSetup(List<Enemy> enemies, EnemyBehavior behavior, String area)
{
    public EncounterSetup
    {
        Objects.requireNonNull(enemies, "enemies must not be null");
        Objects.requireNonNull(behavior, "behavior must not be null");
        Objects.requireNonNull(area, "area must not be null");

        if (enemies.isEmpty())
            throw new IllegalArgumentException("an encounter needs at least one enemy");

        if (area.isBlank())
            throw new IllegalArgumentException("area must not be blank");

        enemies = List.copyOf(enemies);
    }
}
