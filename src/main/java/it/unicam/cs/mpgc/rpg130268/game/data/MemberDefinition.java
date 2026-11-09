package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatRules;

import java.util.List;

// What members.xml says about one party member
record MemberDefinition(String id, StatRules rules, int level, int unspentPoints, StatPoints points, int maxAp,
                        int startingAp, String weaponId, List<String> abilityIds)
{
    MemberDefinition
    {
        abilityIds = List.copyOf(abilityIds);
    }
}
