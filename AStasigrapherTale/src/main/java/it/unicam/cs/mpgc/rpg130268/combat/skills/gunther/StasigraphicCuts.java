package it.unicam.cs.mpgc.rpg130268.combat.skills.gunther;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;

// This ability can be performed only by Gunther.
// To be cast, it requires the user to have at least 4 ink.
// The greater the amount of ink, the greater the damage dealt to the target.
public class StasigraphicCuts implements Ability
{
    public String getName() { return "Stasigraphic Cuts"; }
    public int getApCost() { return 1; }

    public void perform(Fighter user, Fighter target)
    {

    }
}
