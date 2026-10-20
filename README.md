# A Stasigrapher Tale

Java RPG project for the *Metodologie di Programmazione* course (A.Y. 2025/26, University of Camerino).

- **Student:** Matteo Vercelli, Matricola 130268
- **Package:** `it.unicam.cs.mpgc.rpg130268`
- **Documentation:** see the [Wiki](https://github.com/Luv3rc/MDP----A-Stasigrapher-Tale/wiki)

> **Status: work in progress.** The combat data model (fighters, AP, Ink, abilities) is implemented and tested. There is no playable game or user interface yet, so `./gradlew run` currently starts a placeholder `Main`.

## The game
*A Stasigrapher Tale* is a story-driven JRPG with turn-based combat, built around Gunther, one of the last surviving Thesmosians able to wield Stasigraphy.

Fleeing from the Elemental Church, Gunther takes a mercenary job to make ends meet: a desperate woman hires him to rescue her merchant husband, kidnapped by a group of bandits. What starts as a simple rescue turns into a dangerous journey into the woods.

The prologue is planned as:
- **Tutorial:** a first battle that teaches the combat system.
- **Town:** accept the job, take the upfront payment and spend it on items and equipment.
- **Forest and bandit camp:** exploration, combat and levelling, ending with a boss fight.

Combat is turn-based. Gunther's skills cost AP, while basic attacks are free and give AP back. Using Stasigraphy fills an Ink gauge that powers his strongest attacks. When enemies attack, the player reacts with a dodge or a parry based on timing.

## How to run
Requirements: **Java 24**. Gradle is downloaded automatically by the wrapper.
```
./gradlew build
./gradlew run
```

On Windows use `gradlew.bat` or `gradlew build` and `gradlew run` instead of `./gradlew`. To run only the tests:

```
./gradlew test
```

## Use of AI tools
I used **Claude** and **Gemini** during the project. In particular, it helped me to:
- discuss the architecture;
- review my code and find bugs;
- suggest architectural or code changes;
- review code.

The game concept, the story and the mechanics (AP, Ink, Gunther's abilities) are my own, and so are the decisions between the alternatives that were proposed.

## Roadmap

- [x] Fighters, stats, XP and levels (more or less)
- [x] AP and Ink mechanics
- [x] Abilities with costs and requirements
- [ ] Enemy attacks and timed dodge/parry
- [ ] Combat engine
- [ ] Story and exploration
- [ ] Save and load
- [ ] JavaFX interface
