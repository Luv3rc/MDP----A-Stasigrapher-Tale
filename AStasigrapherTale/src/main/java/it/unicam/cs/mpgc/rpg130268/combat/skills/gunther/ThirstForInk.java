package it.unicam.cs.mpgc.rpg130268.combat.skills.gunther;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.partyMembers.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;

// This ability can be performed only by Gunther.
// It puts him at 1hp, but completely refills his AP, fulling his ink gauge
public class ThirstForInk implements Ability
{
    public String getName() { return "Thirst for Ink"; }
    public int getApCost() { return 1; }

    public void perform(Fighter user, Fighter target)
    {
        if (!(user instanceof Gunther gunther))
            throw new IllegalArgumentException("Thirst for Ink can only be used by Gunther");

        if (user != target)
            throw new IllegalArgumentException("Thirst for Ink must be cast on self");

        user.setHp(1);
        gunther.ap().fillAp();
        gunther.ink().fillInk();
    }
}
