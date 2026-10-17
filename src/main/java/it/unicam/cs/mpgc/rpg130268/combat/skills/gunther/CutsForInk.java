package it.unicam.cs.mpgc.rpg130268.combat.skills.gunther;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.skills.PartyAbility;

// This ability can be performed only by Gunther.
// To be cast, it requires the user to have at least 3 AP
// This attack has 3 hits and scales on Strenght
// Each hit gives Gunther 1 ink
public class CutsForInk extends PartyAbility<Gunther>
{
    private static final int HITS = 3;
    private static final int AP_COST = 3;

    public CutsForInk(Gunther user) { super(user); }

    public String getName() { return "Cuts for Ink"; }
    public int getApCost() { return AP_COST; }

    @Override
    protected boolean isValidTarget(Fighter target) { return user != target; }

    @Override
    protected void applyEffect(Fighter target)
    {
        user.getInk().gainInk(HITS * user.getInkRules().defaults().inkGainPerHit());

        // TODO: DamageCalculator
        int damage = (HITS * user.getStats().strength());

        target.takeDamage(damage);
    }
}
