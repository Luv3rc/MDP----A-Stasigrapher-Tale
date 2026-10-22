package it.unicam.cs.mpgc.rpg130268.combat.battle;

/* *
    The ONE clock of a battle. The UI moves it forward (once per frame) and everything else reads it
    (when the events happen, when the player pressed a key, when a hit lands).
    Since everybody uses the same clock, the animations, the inputs and the rules always agree on "when"
*/
public class BattleClock
{
    private long nowMs = 0;

    public long getNowMs() { return nowMs; }

    public void advance(long deltaMs)
    {
        if (deltaMs < 0)
            throw new IllegalArgumentException("deltaMs must not be negative");

        nowMs += deltaMs;
    }
}
