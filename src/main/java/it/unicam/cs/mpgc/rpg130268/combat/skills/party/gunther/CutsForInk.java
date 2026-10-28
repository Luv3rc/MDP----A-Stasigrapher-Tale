package it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.skills.CombatContext;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbility;

import java.util.List;

// This ability can be performed only by Gunther.
// To be cast, it requires the user to have at least 3 AP
// This attack has 3 hits -- 2 light, 1 normal
// Each hit gives Gunther 1 ink
public class CutsForInk extends PartyAbility<Gunther>
{
    private static final int AP_COST = 3;
    private static final String NAME = "Cuts for Ink";
    private static final String DESCRIPTION = "Light damage. 3 hits. Each hit generates 1 INK.";

    private static final TargetType TARGET_TYPE = TargetType.ONE_ENEMY;

    // The exact timing and weight of the 3 strikes. The timing is data for the UI (how long each
    // strike takes to land)
    private static final List<Hit> SEQUENCE = List.of
            (
                    new Hit(400, ReactionOptions.NONE, StrikeWeight.LIGHT),
                    new Hit(200, ReactionOptions.NONE, StrikeWeight.LIGHT),
                    new Hit(600, ReactionOptions.NONE, StrikeWeight.NORMAL)
            );

    public CutsForInk(Gunther user) { super(user, NAME, DESCRIPTION, TARGET_TYPE); }

    @Override
    public int getApCost() { return AP_COST; }

    @Override
    protected void applyEffect(List<Fighter> targets, CombatContext context)
    {
        Fighter target = targets.getFirst();

        for (Hit hit : SEQUENCE)
        {
            // Once the target is dead the combo stops. No more hits and no more ink
            if (!target.isAlive()) break;

            context.resolveHit(user, target, hit);
            user.getInk().gainInk(user.getInkRules().inkGainPerHit());
        }
    }
}