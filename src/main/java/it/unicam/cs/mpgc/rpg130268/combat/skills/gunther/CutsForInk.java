package it.unicam.cs.mpgc.rpg130268.combat.skills.gunther;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.skills.PartyAbility;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;

import java.util.List;

// This ability can be performed only by Gunther.
// To be cast, it requires the user to have at least 3 AP
// This attack has 3 hits and scales on Strength
// Each hit gives Gunther 1 ink
public class CutsForInk extends PartyAbility<Gunther>
{
    private static final int HITS = 3;
    private static final int AP_COST = 3;

    public CutsForInk(Gunther user) { super(user); }

    public String getName() { return "Cuts for Ink"; }
    public int getApCost() { return AP_COST; }
    public TargetType getTargetType() { return TargetType.ONE_ENEMY; }

    @Override
    protected void applyEffect(List<Fighter> targets)
    {
        Fighter target = targets.getFirst();
        user.getInk().gainInk(HITS * user.getInkRules().inkGainPerHit());

        // TODO: DamageCalculator
        int damage = (HITS * user.getStats().strength());
        target.takeDamage(damage);
    }
}
