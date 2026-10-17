package it.unicam.cs.mpgc.rpg130268.combat.skills.gunther;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.skills.PartyAbility;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;

import java.util.List;

// This ability can be performed only by Gunther.
// To be cast, it requires the user to have at least 4 ink and AP.
// The greater the amount of ink, the greater the damage dealt to the target.
// This attack has 4 hits and scales on Agility
public class StasigraphicFury extends PartyAbility<Gunther>
{
    private static final int MIN_INK = 4;
    private static final int HITS = 4;
    private static final int AP_COST = 4;

    public StasigraphicFury(Gunther user) { super(user); }

    public String getName() { return "Stasigraphic Fury"; }
    public int getApCost() { return AP_COST; }
    public TargetType getTargetType() { return TargetType.ONE_ENEMY; }

    @Override
    protected boolean meetsRequirements() { return user.getInk().canAffordInk(MIN_INK); }

    @Override
    protected void applyEffect(List<Fighter> targets)
    {
        Fighter target = targets.getFirst();
        int ink = user.getInk().drainInk();

        // TODO: DamageCalculator
        double inkBonus = 1 + ink / 10.0;
        int damage = (int) (HITS * user.getStats().agility() * inkBonus);

        target.takeDamage(damage);
    }
}
