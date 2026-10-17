package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;

// Base class for abilities that cost AP. It fixes the order of the steps
// (check, pay, execute) so that no ability can forget to pay or pay too early.
// I don't check if the Target isn't a PartyMember because I might want to use
// abilities against them if the user is enchanted
public abstract class PartyAbility<U extends PartyMember> implements Ability
{
    protected final U user;

    protected PartyAbility(U user) { this.user = user; }

    // Used to check if the ability is available right now
    @Override
    public final boolean canPerform()
    {
        return user.getAp().canAffordAbility(getApCost()) && meetsRequirements();
    }

    // Order of the steps is fixed
    @Override
    public final void perform(Fighter target)
    {
        if (!canPerform())
            throw new IllegalStateException(getName() + " cannot be performed");

        if (!isValidTarget(target))
            throw new IllegalArgumentException("Invalid target for " + getName());

        user.getAp().spendAp(getApCost());   // pay only after every check has passed
        applyEffect(target);
    }


    // Might get override by other subclasses
    protected boolean meetsRequirements() { return true; }
    protected boolean isValidTarget(Fighter target) { return true; }

    // What the ability actually does
    // ! MANDATORY
    protected abstract void applyEffect(Fighter target);
}
