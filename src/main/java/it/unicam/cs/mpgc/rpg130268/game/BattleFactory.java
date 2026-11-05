package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleClock;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleRules;
import it.unicam.cs.mpgc.rpg130268.combat.battle.LiveBattleContext;
import it.unicam.cs.mpgc.rpg130268.combat.battle.XpRules;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;

import java.util.List;
import java.util.Objects;

// The one place where a Battle is put together. UI makes its battles here,
// so they all play the same
public final class BattleFactory
{
    private static final long DEAD_ZONE_MS = 1200;          // the pause between the end of an action and the start of the next one
    private static final long ANNOUNCEMENT_MS = 1600;       // how long the text of an ability that announces itself stays

    private static final BattleRules RULES = new BattleRules(StrikeWeight.HEAVY, DEAD_ZONE_MS, ANNOUNCEMENT_MS);
    private static final ReactionWindows WINDOWS = ReactionWindows.defaults();
    private static final XpRules XP_RULES = XpRules.defaults();

    private BattleFactory() { }

    public static Battle create(List<PartyMember> party, EncounterSetup setup)
    {
        Objects.requireNonNull(party, "party must not be null");
        Objects.requireNonNull(setup, "setup must not be null");

        LiveBattleContext context = new LiveBattleContext(new DamageCalculator(), WINDOWS, RULES, new BattleClock());

        return new Battle(party, setup.enemies(), context, setup.behavior(), XP_RULES);
    }

    public static ReactionWindows reactionWindows() { return WINDOWS; }

    public static BattleRules rules() { return RULES; }

    public static XpRules xpRules() { return XP_RULES; }
}
