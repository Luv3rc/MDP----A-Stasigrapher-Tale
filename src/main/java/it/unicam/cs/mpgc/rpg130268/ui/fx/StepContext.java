package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.game.GameFlow;
import it.unicam.cs.mpgc.rpg130268.game.StepOutcome;

// What the screen of a step can ask of the game around it
interface StepContext
{
    // The game in progress
    GameFlow flow();

    void complete(StepOutcome outcome);

    String battleArea();

    void setBattleArea(String area);

    GameScreen mainMenu();
}
