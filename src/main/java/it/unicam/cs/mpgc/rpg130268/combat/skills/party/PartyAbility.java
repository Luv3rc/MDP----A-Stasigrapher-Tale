package it.unicam.cs.mpgc.rpg130268.combat.skills.party;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.skills.AbstractAbility;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;

// Base class for abilities usable by Party Members. Abilities here cost AP
// (check, pay, execute) so that no ability can forget to pay or pay too early.
// I don't check if the Target isn't a PartyMember because I might want to use
// abilities against them if the user is enchanted
public abstract class PartyAbility<USER extends PartyMember> extends AbstractAbility<USER>
{
    protected PartyAbility(USER user, String name, TargetType targetType)
    {
        super(user, name, targetType);
    }

    protected final boolean canAfford() { return user.getAp().canAffordAbility(getApCost()); }

    protected void payCost() { user.getAp().spendAp(getApCost()); }
}
