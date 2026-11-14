package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Remembers the hits of the attacks that are being played and says where the reaction windows of the next one are
final class ReactionCues
{
    private record TimedHit(long startMs, long impactMs, ReactionOptions options) {}

    private final ReactionWindows windows;
    private final List<TimedHit> hitsInProgress = new ArrayList<>();

    ReactionCues(ReactionWindows windows)
    {
        this.windows = Objects.requireNonNull(windows, "windows must not be null");
    }

    // An attack has started
    void remember(BattleEvent.AttackStarted event)
    {
        long start = event.atMs();

        for (Hit hit : event.hits())
        {
            long impact = start + hit.windUpMs();
            hitsInProgress.add(new TimedHit(start, impact, hit.options()));
            start = impact;
        }
    }

    void forgetClosed(long timeMs)
    {
        hitsInProgress.removeIf(hit -> timeMs > hit.impactMs() + lateMaxMs(hit.options()));
    }

    // The next hit that can be reacted to
    Optional<Cue> cueAt(long timeMs)
    {
        for (TimedHit hit : hitsInProgress)
        {
            if (hit.options() == ReactionOptions.NONE) continue;
            if (timeMs < hit.startMs()) return Optional.empty();

            return Optional.of(new Cue(timeMs, hit.startMs(), hit.impactMs(),
                    hit.options().allows(ReactionType.PARRY),
                    hit.impactMs() - windows.earlyParryWindowMs(), hit.impactMs() + windows.lateParryWindowMs(),
                    hit.options().allows(ReactionType.DODGE),
                    hit.impactMs() - windows.earlyDodgeWindowMs(), hit.impactMs() + windows.lateDodgeWindowMs()));
        }

        return Optional.empty();
    }

    private long lateMaxMs(ReactionOptions options)
    {
        return switch (options)
        {
            case NONE -> 0;
            case PARRY_ONLY -> windows.lateParryWindowMs();
            case DODGE_ONLY -> windows.lateDodgeWindowMs();
            case PARRY_OR_DODGE -> Math.max(windows.lateParryWindowMs(), windows.lateDodgeWindowMs());
        };
    }
}
