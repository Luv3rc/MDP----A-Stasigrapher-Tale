package it.unicam.cs.mpgc.rpg130268.combat.skills.gunther;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.skills.PartyAbility;

// This ability can be performed only by Gunther.
// It puts him at 1hp, but completely refills his AP, fulling his ink gauge
public class ThirstForInk extends PartyAbility<Gunther>
{
    private static final int AP_COST = 1;

    public ThirstForInk(Gunther user) { super(user); }

    public String getName() { return "Thirst for Ink"; }
    public int getApCost() { return AP_COST; }

    @Override
    protected boolean isValidTarget(Fighter target) { return user == target; }

    @Override
    protected void applyEffect(Fighter target) {
        user.setHp(1);
        user.getAp().fillAp();
        user.getInk().fillInk();
    }
}