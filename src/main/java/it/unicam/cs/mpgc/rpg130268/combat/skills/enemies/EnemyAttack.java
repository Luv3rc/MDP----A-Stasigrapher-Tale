package it.unicam.cs.mpgc.rpg130268.combat.skills.enemies;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
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
    private final List<Hit> sequence; // The sequence of strikes in this attack

    // With the id chosen on purpose (what enemies.xml does), the attack patterns and the saves point to it
    public EnemyAttack(Enemy user, String id, String name, TargetType targetType, List<Hit> sequence)
    {
        super(user, id, name, targetType);
        this.sequence = checked(sequence);
    }

    // The id is the one the name gives ("Club swing" -> "club-swing"), same as always
    public EnemyAttack(Enemy user, String name, TargetType targetType, List<Hit> sequence)
    {
        super(user, name, targetType);
        this.sequence = checked(sequence);
    }

    private static List<Hit> checked(List<Hit> sequence)
    {
        Objects.requireNonNull(sequence, "sequence must not be null");

        if (sequence.isEmpty())
            throw new IllegalArgumentException("an attack needs at least one hit");

        return List.copyOf(sequence);
    }

    @Override
    protected void applyEffect(List<Fighter> targets, CombatContext context)
    {
        // It is ONE attack on everybody it is aimed at: every hit eats all the living targets together
        // and they react to it together. The context judges the reactions and the parries
        // (e.g. a perfect parry skips the damage and gives the counter)
        List<Fighter> living = targets.stream().filter(Fighter::isAlive).toList();

        if (!living.isEmpty())
            context.resolveAttack(user, living, sequence);
    }
}
