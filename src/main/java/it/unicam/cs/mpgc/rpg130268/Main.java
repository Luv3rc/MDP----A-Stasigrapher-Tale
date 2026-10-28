package it.unicam.cs.mpgc.rpg130268;

import it.unicam.cs.mpgc.rpg130268.ui.fx.BattleApp;
import javafx.application.Application;

// Starts the game. The JavaFX application is a separate class on purpose: a main class that extends
// Application can't be started from the classpath ("JavaFX runtime components are missing")
public class Main
{
    public static void main(String[] args)
    {
        Application.launch(BattleApp.class, args);
    }
}