package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;

import java.util.List;
import java.util.Map;
import java.util.Objects;

// Splits the XP of the defeated enemies among the party (see XpRules) and brings the fallen back with 1 HP
public class XpRewards implements BattleRewards
{
    private final XpRules rules;

    public XpRewards(XpRules rules)
    {
        this.rules = Objects.requireNonNull(rules, "rules must not be null");
    }

    public static XpRewards defaults() { return new XpRewards(XpRules.defaults()); }

    @Override
    public Map<PartyMember, Integer> award(List<PartyMember> party, List<Enemy> defeated)
    {
        int totalXp = defeated.stream().mapToInt(Enemy::getXpReward).sum();
        Map<PartyMember, Integer> shares = rules.split(totalXp, party);

        for (PartyMember member : party)
        {
            member.gainXp(shares.get(member));

            if (!member.isAlive())
                member.setHp(1);
        }

        return shares;
    }
}
