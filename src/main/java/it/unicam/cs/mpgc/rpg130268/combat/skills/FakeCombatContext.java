package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageResult;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageRules;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageSpec;
import it.unicam.cs.mpgc.rpg130268.combat.damage.Scaling;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;

import java.util.List;
import java.util.random.RandomGenerator;

// A CombatContext for the tests: the damage is applied at once, nobody ever reacts and nobody ever crits
// (the roll is always 0.99). It also counts what the abilities asked for
class FakeCombatContext implements CombatContext
{
    private static class FixedRandom implements RandomGenerator
    {
        @Override public double nextDouble() { return 0.99; }
        @Override public long nextLong() { return 0; }
    }

    private final DamageCalculator calculator = new DamageCalculator(DamageRules.defaults(), new FixedRandom());

    int hitsResolved = 0;
    int healsResolved = 0;

    @Override
    public DamageResult resolveHit(Fighter attacker, Fighter target, DamageSpec spec)
    {
        hitsResolved++;

        DamageResult result = calculator.calculate(spec, attacker, target);
        target.takeDamage(result.amount());
        return result;
    }

    @Override
    public int resolveAttack(Fighter attacker, Fighter target, Scaling scaling, List<Hit> hits)
    {
        int total = 0;

        for (Hit hit : hits)
        {
            if (!target.isAlive()) break;

            total += resolveHit(attacker, target, new DamageSpec(scaling, hit.strikeWeight())).amount();
        }

        return total;
    }

    @Override
    public int heal(Fighter source, Fighter target, int amount)
    {
        healsResolved++;

        if (!target.isAlive()) return 0;

        int before = target.getHp();
        target.heal(amount);
        return target.getHp() - before;
    }
}