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
// To be cast, it requires the user to have at least 4 ink and AP.
// The greater the amount of ink, the greater the damage dealt to the target.
// This attack has 4 hits -- 3 light, 1 normal -- and scales on Agility
public class StasigraphicFury extends PartyAbility<Gunther>
{
    private static final int MIN_INK = 4;
    private static final int AP_COST = 4;
    private static final String NAME = "Stasigraphic Fury";
    private static final TargetType TARGET_TYPE = TargetType.ONE_ENEMY;

    private static final Scaling SCALING = Scaling.of(ScalingStat.AGILITY);

    private static final List<Hit> SEQUENCE = List.of
            (
                    new Hit(200, ReactionOptions.NONE, StrikeWeight.LIGHT),
                    new Hit(200, ReactionOptions.NONE, StrikeWeight.LIGHT),
                    new Hit(200, ReactionOptions.NONE, StrikeWeight.LIGHT),
                    new Hit(500, ReactionOptions.NONE, StrikeWeight.NORMAL)
            );

    public StasigraphicFury(Gunther user) { super(user, NAME, TARGET_TYPE); }

    @Override
    public int getApCost() { return AP_COST; }

    @Override
    protected boolean meetsRequirements() { return user.getInk().canAffordInk(MIN_INK); }

    @Override
    protected void applyEffect(List<Fighter> targets, CombatContext context)
    {
        Fighter target = targets.getFirst();
        int ink = user.getInk().drainInk();

        // The dynamic bonus weight is applied by the DamageSpec
        double inkBonus = 1 + ink / 10.0;

        for (Hit hit : SEQUENCE)
        {
            if (!target.isAlive()) break;

            context.resolveHit(user, target, new DamageSpec(SCALING, hit.strikeWeight(), inkBonus));
        }
    }
}