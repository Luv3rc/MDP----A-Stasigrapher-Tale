package it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.skills.CombatContext;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbility;

import java.util.List;

// This ability can be performed only by Gunther.
// It puts him at 1hp, but completely refills his AP, filling his ink gauge
public class ThirstForInk extends PartyAbility<Gunther>
{
    private static final int AP_COST = 1;
    private static final String NAME = "Thirst for Ink";
    private static final String DESCRIPTION = "Puts Gunther at 1hp, but completely refills his AP and his ink gauge.";
    private static final TargetType TARGET_TYPE = TargetType.SELF;

    public ThirstForInk(Gunther user) { super(user, NAME, DESCRIPTION, TARGET_TYPE); }

    @Override
    public int getApCost() { return AP_COST; }

    // It uses nothing from the context: no damage, no reactions
    @Override
    protected void applyEffect(List<Fighter> targets, CombatContext context)
    {
        context.setHp(user, user, 1);
        user.getAp().fillAp();
        user.getInk().fillInk();
    }
}