package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;

import java.util.List;

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
    public final void perform(List<Fighter> targets)    {
        if (!canPerform())
            throw new IllegalStateException(getName() + " cannot be performed");

        validateTargets(targets);

        user.getAp().spendAp(getApCost());   // pay only after every check has passed
        applyEffect(targets);
    }


    // Might get override by other subclasses
    protected boolean meetsRequirements() { return true; }

    // What the ability actually does
    // ! MANDATORY
    protected abstract void applyEffect(List<Fighter> targets);

    // The ability only checks what it can know: how many targets, and whether the user is one of them.
    // Choosing the correct SIDE (who the enemies are) is the engine's job, because it builds the list.
    private void validateTargets(List<Fighter> targets)
    {
        if (targets == null || targets.isEmpty())
            throw new IllegalArgumentException(getName() + " needs at least one target");

        switch (getTargetType())
        {
            case SELF ->
            {
                if (targets.size() != 1 || targets.getFirst() != user) // was get(0)
                    throw new IllegalArgumentException(getName() + " must target only its user");
            }
            case ONE_ENEMY ->
            {
                if (targets.size() != 1 || targets.contains(user))
                    throw new IllegalArgumentException(getName() + " needs exactly one target, other than the user");
            }
            case ONE_ALLY ->
            {
                if (targets.size() != 1)
                    throw new IllegalArgumentException(getName() + " needs exactly one target");
            }
            case ALL_ENEMIES ->
            {
                if (targets.contains(user))
                    throw new IllegalArgumentException(getName() + " cannot target its user");
            }
            case ALL_ALLIES -> { }
        }
    }
}
