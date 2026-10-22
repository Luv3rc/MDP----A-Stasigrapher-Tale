package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAbility;

import java.util.List;
import java.util.Objects;

// What an enemy decided to do in its turn, which ability and on whom
public record EnemyAction(EnemyAbility ability, List<Fighter> targets)
{
    public EnemyAction
    {
        Objects.requireNonNull(ability, "ability must not be null");
        Objects.requireNonNull(targets, "targets must not be null");

        if (targets.isEmpty())
            throw new IllegalArgumentException("targets must not be empty");

        targets = List.copyOf(targets);
    }
}
