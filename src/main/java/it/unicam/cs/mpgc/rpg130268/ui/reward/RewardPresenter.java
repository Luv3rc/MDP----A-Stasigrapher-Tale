package it.unicam.cs.mpgc.rpg130268.ui.reward;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.game.MemberReward;
import it.unicam.cs.mpgc.rpg130268.game.Reward;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/* *
    For every member of the party it has his name, his level (and if it went up), the XP he got in the battle, the XP
    bar (how much of the next level is already earned), the points he has to spend at a campfire and his stats
    Over all of them there is the XP of the battle
* */
public final class RewardPresenter
{
    public record StatView(String label, int value, double fraction) { }

    // xp and xpForNext are the earned part of the next level. xpForNext is 0 at the highest level, and then the bar is full
    public record MemberView(String name, String artName, int level, boolean leveledUp, int xpGained, int xp,
                             int xpForNext, double xpFraction, int pointsGained, int unspentPoints,
                             List<StatView> stats) { }

    public record View(String title, String totalXp, List<MemberView> members) { }

    private final List<PartyMember> party;
    private final Reward reward;

    public RewardPresenter(List<PartyMember> party, Reward reward)
    {
        Objects.requireNonNull(party, "party must not be null");
        Objects.requireNonNull(reward, "reward must not be null");

        for (MemberReward entry : reward.members())
            if (party.stream().noneMatch(member -> member.getId().equals(entry.memberId())))
                throw new IllegalArgumentException("the reward is for \"" + entry.memberId() + "\", who is not in the party");

        this.party = List.copyOf(party);
        this.reward = reward;
    }

    public View view()
    {
        return new View("VICTORY", NumberFormat.getIntegerInstance(Locale.US).format(reward.totalXp()),
                reward.members().stream().map(this::memberView).toList());
    }

    private MemberView memberView(MemberReward entry)
    {
        PartyMember member = party.stream().filter(candidate -> candidate.getId().equals(entry.memberId())).findFirst().orElseThrow();
        int forNext = member.getXpForNextLevel();
        double xpFraction = forNext == 0 ? 1 : fraction(member.getCurrentXp(), forNext);

        return new MemberView(member.getName(), member.getArtName(), member.getLevel(), entry.leveledUp(), entry.xpGained(),
                member.getCurrentXp(), forNext, xpFraction, entry.pointsGained(), member.getUnspentPoints(),
                statsOf(member.getStats(), member.getStatRules()));
    }

    private static List<StatView> statsOf(Stats stats, StatRules rules)
    {
        int healthScale = rules.baseHealth() + rules.statCap() * rules.healthPerPoint();

        return List.of(
                new StatView("Max health", stats.maxHealth(), fraction(stats.maxHealth(), healthScale)),
                new StatView("Strength", stats.strength(), fraction(stats.strength(), rules.statCap())),
                new StatView("Defense", stats.defense(), fraction(stats.defense(), rules.statCap())),
                new StatView("Agility", stats.agility(), fraction(stats.agility(), rules.statCap())),
                new StatView("Luck", stats.luck(), fraction(stats.luck(), rules.statCap())));
    }

    private static double fraction(int value, int scale)
    {
        return Math.max(0, Math.min(1, (double) value / scale));
    }
}
