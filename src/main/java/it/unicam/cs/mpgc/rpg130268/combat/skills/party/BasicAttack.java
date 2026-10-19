package it.unicam.cs.mpgc.rpg130268.combat.skills.party;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.skills.CombatContext;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;

import java.util.List;

public class BasicAttack extends PartyAbility<PartyMember>
{
    private static final short AP_COST = 0;
    private static final String NAME = "Basic Attack";
    private static final TargetType TARGET_TYPE = TargetType.ONE_ENEMY;
    private static final short WIND_UP_MS = 400;

    private static final Hit STRIKE = new Hit(WIND_UP_MS, ReactionOptions.NONE, StrikeWeight.NORMAL);

    public BasicAttack(PartyMember user) { super(user, NAME, TARGET_TYPE); }

    @Override
    public int getApCost() { return AP_COST; }

    @Override
    protected void applyEffect(List<Fighter> targets, CombatContext context)
    {
        Fighter target = targets.getFirst();

        if (!target.isAlive())
            return;

        context.resolveHit(user, target, STRIKE);
        // Grants 1 AP
        // Gunther override it to gain 1 ink too
        user.onBasicAttackLanded();
    }
}
