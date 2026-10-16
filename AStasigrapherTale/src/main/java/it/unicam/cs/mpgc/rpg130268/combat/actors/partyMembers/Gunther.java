package it.unicam.cs.mpgc.rpg130268.combat.actors.partyMembers;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.LevelRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkPool;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;

// TODO
public class Gunther extends PartyMember
{
    private final InkPool ink;

    public Gunther(Stats stats, int maxAp, int startingAp, ApRules apRules)
    {
        super("Gunther", stats, maxAp, startingAp, apRules, LevelRules.defaults(), 1, 0);
        this.ink = new InkPool(8, 0, InkRules.defaults());
    }

    public void onLevelUp()
    {
        // TODO
    }

    public InkPool ink() { return ink; }

}
