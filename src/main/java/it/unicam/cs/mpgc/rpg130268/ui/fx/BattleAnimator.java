package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOutcome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/* *
    Turns what happens in the battle (the BattleEvents) into what the player sees: the cuts of the attacks, the
    numbers over the sprites, the shakes, the strikes of the attackers, the announcements and the final banner.
    It does not decide anything, and it does not know the keys or the menu.

    Time is the clock of the battle (not the one of the window): a blade that has to land at the millisecond 3200 of
    the battle lands there, whatever the frame rate is.
* */
final class BattleAnimator
{
    private static final String DAMAGE_COLOR = "#e74c3c";    // the colors of the numbers over the sprites
    private static final String CRIT_COLOR = "#e056fd";      // a critical hit: not like any other number

    // The moment (battle clock) when a fighter has to start its attack animation so that its blow lands with the hit
    private record AttackCue(long atMs, FighterCard card) { }

    private final Map<Fighter, FighterCard> cards;
    private final SlashEffect slashes;
    private final BattleOverlay overlay;
    private final LongSupplier clock;                     // the time of the battle, in ms
    private final List<AttackCue> attackCues = new ArrayList<>();

    BattleAnimator(Map<Fighter, FighterCard> cards, SlashEffect slashes, BattleOverlay overlay, LongSupplier clock)
    {
        this.cards = Objects.requireNonNull(cards, "cards must not be null");
        this.slashes = Objects.requireNonNull(slashes, "slashes must not be null");
        this.overlay = Objects.requireNonNull(overlay, "overlay must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // A new battle: nothing of the old one is left
    void reset()
    {
        attackCues.clear();
        slashes.clear();
        overlay.reset();
    }

    // Called every frame: the blades move, and the fighters whose moment has come start their strike
    void update()
    {
        long now = clock.getAsLong();

        slashes.update(now);

        attackCues.removeIf(cue ->
        {
            if (cue.atMs() > now) return false;

            cue.card().attack();
            return true;
        });
    }

    void animate(BattleEvent event)
    {
        switch (event)
        {
            case BattleEvent.HitResolved e ->
            {
                slashEffect(e);
                hitEffect(e);
            }
            case BattleEvent.Countered e ->
            {
                FighterCard card = cards.get(e.attacker());
                if (card != null)
                {
                    slashes.cut(e.attacker(), card.getTarget(), e.critical(), isParty(e.defender()), clock.getAsLong());
                    card.floatText("COUNTER -" + e.damage() + (e.critical() ? "!" : ""), e.critical() ? CRIT_COLOR : "#e67e22", e.critical() ? 30 : 22);
                    card.shake();
                }
            }
            case BattleEvent.Healed e ->
            {
                FighterCard card = cards.get(e.target());
                if (card != null) card.floatText("+" + e.amount(), "#2ecc71", 22);
            }
            case BattleEvent.BattleEnded e -> overlay.showOutcome(e);
            case BattleEvent.TurnStarted e -> { }
            case BattleEvent.Announced e -> overlay.announce(e);
            case BattleEvent.AttackStarted e -> scheduleSlashes(e);
            case BattleEvent.Swing e -> scheduleSwing(e);
            case BattleEvent.Defeated e ->
            {
                FighterCard card = cards.get(e.fighter());
                if (card != null) card.defeat();
            }
        }
    }

    // An enemy attack has begun: every hit has its blade, which will be on its target exactly when the hit lands.
    // The hit i starts when the hit i-1 lands (the same rule the presenter uses for the timing bar)
    private void scheduleSlashes(BattleEvent.AttackStarted e)
    {
        boolean fromParty = isParty(e.attacker());
        long start = e.atMs();

        for (Hit hit : e.hits())
        {
            long impact = start + hit.windUpMs();

            for (Fighter target : e.targets())
            {
                FighterCard card = cards.get(target);
                if (card != null)
                    slashes.schedule(e.attacker(), target, card.getTarget(), start, impact, hit.strikeWeight(), fromParty);
            }

            queueAttack(e.attacker(), impact);
            start = impact;
        }
    }

    // A strike of the party (it cannot be reacted to): the blade comes toward the target during the wind-up, and the
    // attacker strikes so that its blow lands with it. Then the HitResolved says what happened
    private void scheduleSwing(BattleEvent.Swing e)
    {
        boolean fromParty = isParty(e.attacker());
        long impact = e.atMs() + e.hit().windUpMs();

        for (Fighter target : e.targets())
        {
            FighterCard card = cards.get(target);
            if (card != null)
                slashes.schedule(e.attacker(), target, card.getTarget(), e.atMs(), impact, e.hit().strikeWeight(), fromParty);
        }

        queueAttack(e.attacker(), impact);
    }

    // The attacker's strike starts a bit before the impact: as long as its animation needs to reach the blow
    private void queueAttack(Fighter attacker, long impactMs)
    {
        FighterCard card = cards.get(attacker);
        if (card != null)
            attackCues.add(new AttackCue(impactMs - card.attackLeadMs(), card));
    }

    // The hit has been judged: its blade learns how it ended (or a fast cut is drawn, for the strikes of the player)
    private void slashEffect(BattleEvent.HitResolved e)
    {
        FighterCard card = cards.get(e.target());
        if (card == null) return;

        ReactionOutcome reaction = e.reaction();
        SlashEffect.Outcome outcome = reaction.isSuccessfulParry() ? SlashEffect.Outcome.PARRIED
                : reaction.avoidsDamage() ? SlashEffect.Outcome.DODGED
                  : SlashEffect.Outcome.HIT;

        slashes.resolve(e.attacker(), e.target(), card.getTarget(), outcome, e.critical(), isParty(e.attacker()), clock.getAsLong());
    }

    private void hitEffect(BattleEvent.HitResolved e)
    {
        FighterCard card = cards.get(e.target());
        if (card == null) return;

        if (e.reaction().isSuccessfulParry())
        {
            card.floatText("PARRY!", "#f1c40f", 24);
            card.play("parry");
        }
        else if (e.reaction().avoidsDamage())
        {
            card.floatText("DODGE!", "#3498db", 24);
            card.sidestep();
            card.play("dodge");
        }
        else
        {
            card.floatText("-" + e.damage() + (e.critical() ? "!" : ""), e.critical() ? CRIT_COLOR : DAMAGE_COLOR, e.critical() ? 34 : 22);
            card.shake();
            card.play("hit");
        }
    }

    private boolean isParty(Fighter fighter)
    {
        FighterCard card = cards.get(fighter);
        return card != null && card.getView().isPartyMember();
    }
}