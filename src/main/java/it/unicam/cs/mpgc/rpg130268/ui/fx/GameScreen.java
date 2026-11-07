package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

// A full screen of the game. A screen draws what its presenter says and tells it what the player does,
// without any game logic of its own.
interface GameScreen
{
    // What to draw. The host gives it the size of the design (1280 x 720) and scales it to the window
    Region getRoot();

    void onKeyPressed(KeyEvent event);

    default void onShow() { }

    default void stop() { }
}
