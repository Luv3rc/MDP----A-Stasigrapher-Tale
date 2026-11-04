package it.unicam.cs.mpgc.rpg130268.game;

// A fight waiting to happen. Every call to spawn() makes new enemies at full
// health, so the same encounter can be played again (after a defeat) or met many times
@FunctionalInterface
public interface Encounter
{
    EncounterSetup spawn();
}
