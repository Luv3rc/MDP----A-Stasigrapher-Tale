package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;
import java.util.List;
import java.util.Objects;

// It fixes the order of the steps
public abstract class AbstractAbility<USER extends Fighter> implements Ability
{
    protected final USER user;
    private final String abilityName;
    private final TargetType targetType;

    protected AbstractAbility (USER user, String abilityName, TargetType targetType)
    {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(abilityName, "name must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");

        this.user = user;
        this.abilityName = abilityName;
        this.targetType = targetType;
    }

    @Override
    public String getName() { return abilityName; }
    public TargetType getTargetType() { return targetType; }

    // Used to check if the ability is available right now. Final: every ability, party or enemy,
    // is available only if its user can pay AND its own requirements are met
    @Override
    public final boolean canPerform() { return canAfford() && meetsRequirements(); }

    // Order of the steps is fixed -- Moved from PartyAbility
    public final void perform(List<Fighter> targets, CombatContext context)
    {
        Objects.requireNonNull(context, "context must not be null");

        if (!canPerform())
            throw new IllegalStateException(abilityName + " cannot be performed");

        validateTargets(targets);
        payCost();
        applyEffect(targets, context);
    }

    // What the cost is depends on who uses the ability (AP for the party, nothing for enemies)
    protected abstract boolean canAfford();
    protected abstract void payCost();
    protected abstract void applyEffect(List<Fighter> targets, CombatContext context);


    // Might get override by other subclasses (e.g. "only when wounded", "needs 4 ink")
    protected boolean meetsRequirements() { return true; }

    // The ability only checks what it can know: how many targets, and whether the user is one of them.
    // Choosing the correct SIDE (who the enemies are) is the engine's job, because it builds the list.
    private void validateTargets(List<Fighter> targets)
    {
        Objects.requireNonNull(targets, "targets cannot be null");
        if (targets.isEmpty())
            throw new IllegalArgumentException("targets must not be empty");

        switch (targetType)
        {
            case SELF ->
            {
                if (targets.size() != 1 || targets.getFirst() != user)
                    throw new IllegalArgumentException(abilityName + " must target only its user");
            }
            case ONE_ENEMY ->
            {
                if (targets.size() != 1 || targets.contains(user))
                    throw new IllegalArgumentException(abilityName + " needs exactly one target, other than the user");
            }
            case ONE_ALLY ->
            {
                if (targets.size() != 1)
                    throw new IllegalArgumentException(abilityName + " needs exactly one target");
            }
            case ALL_ENEMIES ->
            {
                if (targets.contains(user))
                    throw new IllegalArgumentException(abilityName + " cannot target its user");
            }
            case ALL_ALLIES -> { }
        }
    }
}
