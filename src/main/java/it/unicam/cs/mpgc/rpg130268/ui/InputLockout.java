package it.unicam.cs.mpgc.rpg130268.ui;

// A penalty for pressing keys at random. When a key counts for nothing the UI calls punish(), and until the
// penalty is over every key must be ignored (the UI doesn't even pass them to the battle).
// It only needs the time the UI is using (the battle clock), so it works the same in a console or a window
public class InputLockout
{
    private final long penaltyMs;
    private long lockedUntilMs = 0;

    public InputLockout(long penaltyMs)
    {
        if (penaltyMs < 0)
            throw new IllegalArgumentException("penaltyMs must not be negative");

        this.penaltyMs = penaltyMs;
    }

    public long getPenaltyMs() { return penaltyMs; }

    // True while the penalty lasts: every key must be ignored
    public boolean isLocked(long nowMs) { return nowMs < lockedUntilMs; }

    // How long until the keys count again (0 if they already do)
    public long remainingMs(long nowMs) { return Math.max(0, lockedUntilMs - nowMs); }

    // A key counted for nothing: the keys are locked for penaltyMs from now
    public void punish(long nowMs) { lockedUntilMs = nowMs + penaltyMs; }
}