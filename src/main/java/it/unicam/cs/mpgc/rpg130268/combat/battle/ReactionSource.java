package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionInput;

import java.util.List;

// Where the reactions of the fighters come from. The UI implements it (it reads the keyboard),
// the tests implement it with a script. The engine never knows who is behind it
@FunctionalInterface // It's Functional now to allow Lambda expression for tests, might change later if needed
public interface ReactionSource
{
    // What the targets did about this hit. A hit eats EVERYONE it is aimed at, so there is ONE input for all
    // of them (a single target attack has a list of one). timeMs is measured from the start of the hit's
    // wind-up, so hit.windUpMs() is the duration of the strike
    // Fighters nobody controls (e.g. enemies being attacked) simply do nothing
    ReactionInput inputFor(List<Fighter> targets, Hit hit);

    // Nobody ever reacts
    static ReactionSource none() { return (targets, hit) -> ReactionInput.none(); }
}
