package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;

import java.util.Objects;

// The keys of the player during an enemy attack, and the lockout for pressing them at random
final class ReactionInput
{
    private final Battle battle;
    private final InputLockout lockout;
    private final BattleLog log;

    ReactionInput(Battle battle, InputLockout lockout, BattleLog log)
    {
        this.battle = Objects.requireNonNull(battle, "battle must not be null");
        this.lockout = Objects.requireNonNull(lockout, "lockout must not be null");
        this.log = Objects.requireNonNull(log, "log must not be null");
    }

    void press(ReactionType type, long nowMs)
    {
        Objects.requireNonNull(type, "type must not be null");

        if (!battle.isAttackInProgress()) return;

        if (lockout.isLocked(nowMs)) return;

        if (!battle.react(type))
        {
            lockout.punish(nowMs);
            log.add("   Missed! Your keys are locked for " + lockout.getPenaltyMs() + " ms.");
        }
    }

    boolean isLocked(long nowMs) { return lockout.isLocked(nowMs); }

    long remainingMs(long nowMs) { return lockout.remainingMs(nowMs); }
}
