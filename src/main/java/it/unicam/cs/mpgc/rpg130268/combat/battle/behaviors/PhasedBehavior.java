package it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.EnemyAction;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// A boss that changes as it gets hurt. It follows a behavior, and from the moment its health goes down to a
// fraction of the maximum it follows another one. With several phases, the one with the lowest fraction that
// applies wins:
// * new PhasedBehavior(calm).below(0.5, angry).below(0.2, desperate)
public class PhasedBehavior implements EnemyBehavior
{
    private record Phase(double belowFraction, EnemyBehavior behavior) { }

    private final List<Phase> phases = new ArrayList<>();

    // The behavior of a healthy enemy (it applies to any health)
    public PhasedBehavior(EnemyBehavior first)
    {
        phases.add(new Phase(1.0, Objects.requireNonNull(first, "the first behavior must not be null")));
    }

    // From the moment its health is at or below this fraction of the maximum (0.5 = half)
    public PhasedBehavior below(double hpFraction, EnemyBehavior behavior)
    {
        Objects.requireNonNull(behavior, "behavior must not be null");

        if (hpFraction <= 0 || hpFraction >= 1)
            throw new IllegalArgumentException("the fraction of health must be between 0 and 1, both excluded");

        phases.add(new Phase(hpFraction, behavior));
        return this;
    }

    @Override
    public Optional<EnemyAction> choose(Enemy self, List<Fighter> allies, List<Fighter> foes)
    {
        double health = (double) self.getHp() / self.getStats().maxHealth();

        // The first phase applies to everything, so there is always one
        Phase current = phases.stream()
                .filter(phase -> health <= phase.belowFraction())
                .min(Comparator.comparingDouble(Phase::belowFraction))
                .orElseThrow();

        return current.behavior().choose(self, allies, foes);
    }
}