package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/* *
    How the XP of a won battle is shared in the party
    Everybody gets the same part of it. A fallen member gets a part that is smaller by "fallenPenalty" (20% for default),
    and what he loses goes to the ones who are standing, who share it
    e.g. 100 XP, two members, one fallen, 20%:  each part is 50; the fallen gets 40, the other gets 50 + 10 = 60
* */
public record XpRules(double fallenPenalty)
{
    public XpRules
    {
        if (fallenPenalty < 0 || fallenPenalty > 1)
            throw new IllegalArgumentException("fallenPenalty must be between 0 and 1");
    }

    public static XpRules defaults() { return new XpRules(0.20); }

    // The XP of every member of the party
    public Map<PartyMember, Integer> split(int totalXp, List<PartyMember> party)
    {
        Objects.requireNonNull(party, "party must not be null");

        if (totalXp < 0)
            throw new IllegalArgumentException("totalXp must not be negative");

        Map<PartyMember, Integer> shares = new LinkedHashMap<>();
        List<PartyMember> standing = party.stream().filter(PartyMember::isAlive).toList();

        if (standing.isEmpty() || totalXp == 0)
        {
            party.forEach(member -> shares.put(member, 0));
            return shares;
        }

        int part = totalXp / party.size();
        int given = 0;

        for (PartyMember member : party)
        {
            int share = member.isAlive() ? part : (int) (part * (1 - fallenPenalty));

            shares.put(member, share);
            given += share;
        }

        // What the fallen lost, and the remainder goes to the ones standing
        int left = totalXp - given;

        for (int i = 0; i < standing.size(); i++)
            shares.merge(standing.get(i), left / standing.size() + (i < left % standing.size() ? 1 : 0), Integer::sum);

        return shares;
    }
}
