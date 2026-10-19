package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionInput;

// Where the reactions of the fighters come from. The UI implements it (it reads the keyboard),
// the tests implement it with a script. The engine never knows who is behind it
@FunctionalInterface // It's Functional now to allow Lambda expression for tests, might change later if needed
public interface ReactionSource
{
    // What the target did about this hit. timeMs is measured from the start of the hit's wind-up,
    // so hit.windUpMs() is the duration of the strike
    // A fighter nobody controls (e.g. an enemy being attacked) simply does nothing
    ReactionInput inputFor(Fighter target, Hit hit);

    // Nobody ever reacts
    static ReactionSource none() { return (target, hit) -> ReactionInput.none(); }
}
