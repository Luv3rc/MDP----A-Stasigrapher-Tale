package it.unicam.cs.mpgc.rpg130268;

import it.unicam.cs.mpgc.rpg130268.ui.fx.BattleApp;
import it.unicam.cs.mpgc.rpg130268.ui.fx.GameApp;
import javafx.application.Application;

import java.util.List;

// Starts the game from the main menu. With --battle it opens only a demo battle, to try it
public class Main
{
    public static void main(String[] args)
    {
        boolean onlyBattle = List.of(args).contains("--battle");

        Application.launch(onlyBattle ? BattleApp.class : GameApp.class, args);
    }
}
