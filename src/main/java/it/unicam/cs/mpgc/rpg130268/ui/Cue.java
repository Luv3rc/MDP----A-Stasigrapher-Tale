package it.unicam.cs.mpgc.rpg130268.ui;

// Where the reaction windows of the next hit are, on the battle clock, for drawing a timing bar
public record Cue(long nowMs, long startMs, long impactMs, boolean parry, long parryFromMs, long parryToMs,
                  boolean dodge, long dodgeFromMs, long dodgeToMs) {}
