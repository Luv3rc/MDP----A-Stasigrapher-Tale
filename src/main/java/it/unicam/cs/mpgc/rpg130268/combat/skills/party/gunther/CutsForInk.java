package it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageSpec;
import it.unicam.cs.mpgc.rpg130268.combat.damage.Scaling;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.skills.CombatContext;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbility;

import java.util.List;

// This ability can be performed only by Gunther.
// To be cast, it requires the user to have at least 3 AP
// This attack has 3 hits -- 2 light, 1 normal -- and scales on Strength
// Each hit gives Gunther 1 ink
public class CutsForInk extends PartyAbility<Gunther>
{
    private static final int AP_COST = 3;
    private static final String NAME = "Cuts for Ink";
    private static final TargetType TARGET_TYPE = TargetType.ONE_ENEMY;

    private static final Scaling SCALING = Scaling.of(ScalingStat.STRENGTH);

    // The exact timing and weight of the 3 strikes. The timing is data for the UI (how long each
    // strike takes to land)
    private static final List<Hit> SEQUENCE = List.of
            (
                    new Hit(400, ReactionOptions.NONE, StrikeWeight.LIGHT),
                    new Hit(200, ReactionOptions.NONE, StrikeWeight.LIGHT),
                    new Hit(600, ReactionOptions.NONE, StrikeWeight.NORMAL)
            );

    public CutsForInk(Gunther user) { super(user, NAME, TARGET_TYPE); }

    @Override
    public int getApCost() { return AP_COST; }

    @Override
    protected void applyEffect(List<Fighter> targets, CombatContext context)
    {
        Fighter target = targets.getFirst();

        for (Hit hit : SEQUENCE)
        {
            // Once the target is dead the combo stops: no more hits and no more ink
            if (!target.isAlive()) break;

            context.resolveHit(user, target, new DamageSpec(SCALING, hit.strikeWeight()));
            user.getInk().gainInk(user.getInkRules().inkGainPerHit());
        }
    }
}