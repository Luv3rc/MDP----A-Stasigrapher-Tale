package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.Ids;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/* *
    Everything that makes a party member different from the one he was at the start of the game: how far he is
    (level, xp, hp), what he did with his points, and what he carries (his weapon and his abilities, by id).
    A PartyMember cannot be told "you are level 3 now", so loading a game or retrying a battle means making him
    again from this (see MemberCatalog.create). The saves are made of these.
* */
public record MemberState(String id, int level, int xp, int hp, StatPoints points, int unspentPoints,
                          String weaponId, List<String> abilityIds)
{
    public MemberState
    {
        Ids.require(id);
        Objects.requireNonNull(points, "points must not be null");
        Ids.require(weaponId);
        Objects.requireNonNull(abilityIds, "abilityIds must not be null");

        if (level < 1)
            throw new IllegalArgumentException("level must be at least 1");
        if (xp < 0)
            throw new IllegalArgumentException("xp must not be negative");
        if (hp < 0)
            throw new IllegalArgumentException("hp must not be negative");
        if (unspentPoints < 0)
            throw new IllegalArgumentException("unspentPoints must not be negative");

        abilityIds = List.copyOf(abilityIds);       // it also refuses null ids

        Set<String> seen = new HashSet<>();
        for (String ability : abilityIds)
            if (!seen.add(Ids.require(ability)))
                throw new IllegalArgumentException("the ability \"" + ability + "\" is there twice");
    }

    // The state of a member right now. He must carry a weapon of the game (every member of members.xml does)
    public static MemberState of(PartyMember member)
    {
        Objects.requireNonNull(member, "member must not be null");

        return new MemberState(member.getId(), member.getLevel(), member.getCurrentXp(), member.getHp(),
                member.getStatPoints(), member.getUnspentPoints(), member.getWeapon().id(),
                member.getListOfAbility().stream().map(ability -> ability.getId()).toList());
    }
}
