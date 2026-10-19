package it.unicam.cs.mpgc.rpg130268.combat.skills.enemies;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.damage.Scaling;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.skills.CombatContext;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;

import java.util.List;
import java.util.Objects;

// A sequence of hits aimed at the targets. Each hit tells how long the player has to react and how.
// Every attack of every enemy is an OBJECT of this class (a goblin's "Club swing" and a boss's "Roar"
// are two instances with different data)
public class EnemyAttack extends EnemyAbility
{
    private final Scaling scaling;
    private final List<Hit> sequence; // The sequence of strikes in this attack

    public EnemyAttack(Enemy user, String name, TargetType targetType, Scaling scaling, List<Hit> sequence)
    {
        super(user, name, targetType);

        Objects.requireNonNull(scaling, "scaling must not be null");
        Objects.requireNonNull(sequence, "sequence must not be null");

        if (sequence.isEmpty())
            throw new IllegalArgumentException("an attack needs at least one hit");

        this.scaling = scaling;
        this.sequence = List.copyOf(sequence);
    }

    @Override
    protected void applyEffect(List<Fighter> targets, CombatContext context)
    {
        // Every target is hit by the whole sequence. The context waits for the reactions
        // and parries (e.g. a perfect parry skips the damage and gives the counter)
        for (Fighter target : targets)
            if (target.isAlive())
                context.resolveAttack(user, target, scaling, sequence);
    }
}
