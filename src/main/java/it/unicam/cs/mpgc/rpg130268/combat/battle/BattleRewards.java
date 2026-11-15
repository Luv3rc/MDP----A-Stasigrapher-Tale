package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;

import java.util.List;
import java.util.Map;

public interface BattleRewards
{
    // Returns the XP each member got
    Map<PartyMember, Integer> award(List<PartyMember> party, List<Enemy> defeated);
}
