package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.scene.Parent;
import javafx.scene.control.Button;

import java.net.URL;

// The look of the buttons (src/main/resources/css/battle.css), for every screen that has them.
// If the file is not there the buttons are plain and the game works anyway
final class GameStyle
{
    private static final URL STYLESHEET = GameStyle.class.getResource("/css/battle.css");

    private GameStyle() { }

    static boolean apply(Parent root)
    {
        if (STYLESHEET == null)
            return false;

        root.getStylesheets().add(STYLESHEET.toExternalForm());
        return true;
    }

    static void setSelected(Button button, boolean selected)
    {
        button.getStyleClass().remove("rpg-selected");

        if (selected)
            button.getStyleClass().add("rpg-selected");
    }
}
