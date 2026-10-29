package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Locale;

/* *
    Makes the buttons of the battle: an elongated hexagon, a thin silver border and a dark inside (see battle.css).
    It is two hexagons one in the other: the button itself is the silver one (its padding is the border) and
    what it holds, the inside, is a dark one a bit smaller. The stylesheet gives the colors, the hover and the
    disabled look; the shape is calculated here for the real size.
    If the stylesheet is missing ("styled" is false) the button is a plain dark box: the game works anyway.
* */
final class RpgButtons
{
    private static final double MENU_BUTTON_WIDTH = 210;
    private static final double MENU_BUTTON_HEIGHT = 52;
    private static final double ABILITY_WIDTH = 272;              // with more than three abilities: ABILITY_WIDTH_NARROW
    private static final double ABILITY_WIDTH_NARROW = 262;       // (two columns must not reach Gunther)
    private static final double ABILITY_HEIGHT = 75;              // three of them (with the gaps) are not taller than Gunther
    private static final double BORDER = 1;                       // the thin border of a button (see battle.css)

    private static final String BUTTON_STYLE = "-fx-background-color: #14141d; -fx-text-fill: white; -fx-font-size: 20px; "
            + "-fx-border-color: #e8e8f0; -fx-border-width: 2; -fx-background-radius: 0; -fx-border-radius: 0;";

    private final boolean styled;       // battle.css found: the buttons are hexagons with a silver border

    RpgButtons(boolean styled)
    {
        this.styled = styled;
    }

    // A button of the main menu: just its text
    Button menu(String text)
    {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #f5ebd0; -fx-font-size: 14px; -fx-font-weight: bold;");

        return rpgButton(label, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT, Pos.CENTER);
    }

    // The name with its key and AP cost, and under it what the ability does. The size is fixed: a long description
    // is cut with "..." (the whole text is in the tooltip). "narrow" is for the menus with two columns
    Button ability(String key, Ability ability, boolean narrow)
    {
        double width = narrow ? ABILITY_WIDTH_NARROW : ABILITY_WIDTH;

        // The text has room around it: the same on every side of the button, and the pointed ends are kept clear
        double left = 0.6 * bevel(ABILITY_HEIGHT) + 16;
        double right = 0.6 * bevel(ABILITY_HEIGHT) + 12;
        double textWidth = width - 2 * BORDER - left - right;

        Label name = new Label("[" + key + "]  " + ability.getName() + "   " + "(" + ability.getApCost() + ")");
        name.setStyle("-fx-text-fill: #f5ebd0; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label description = new Label(ability.getDescription());
        description.setStyle("-fx-text-fill: #b8b8cc; -fx-font-size: 11px;");
        description.setWrapText(true);
        description.setPrefWidth(textWidth);
        description.setMaxWidth(textWidth);
        description.setMinHeight(Region.USE_PREF_SIZE); // Forces JavaFX to allocate enough height for wrapped lines
        description.setAlignment(Pos.TOP_LEFT);       // two lines (the whole text is in the tooltip)

        // The text starts at the top of the button, always at the same height: the name is where the name is on every
        // button, whatever the length of the description is
        VBox text = new VBox(4, name, description);
        text.setAlignment(Pos.TOP_LEFT);

        return rpgButton(text, width, ABILITY_HEIGHT, Pos.TOP_LEFT, new Insets(8, right, 10, left));
    }

    private Button rpgButton(Node content, double width, double height, Pos alignment)
    {
        double bevel = bevel(height);
        return rpgButton(content, width, height, alignment, new Insets(0, bevel * 0.6 + 8, 0, bevel * 0.6 + 14));
    }

    // padding: the room between the text and the border of the button
    private Button rpgButton(Node content, double width, double height, Pos alignment, Insets padding)
    {
        Button button = new Button();
        button.setFocusTraversable(false);       // the keys are the screen's, a focused button must not take them
        fixSize(button, width, height);

        StackPane inner = new StackPane(content);
        inner.setAlignment(alignment);
        inner.setPadding(padding);
        fixSize(inner, width - 2 * BORDER, height - 2 * BORDER);

        if (styled)
        {
            button.getStyleClass().add("rpg-button");
            button.setStyle(shape(width, height));
            inner.getStyleClass().add("rpg-button-inner");
            inner.setStyle(shape(width - 2 * BORDER, height - 2 * BORDER));
        }
        else
            button.setStyle(BUTTON_STYLE);

        button.setGraphic(inner);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        // A small press: the button gets a bit smaller while it is held down
        button.pressedProperty().addListener((observable, was, pressed) ->
        {
            button.setScaleX(pressed ? 0.98 : 1.0);
            button.setScaleY(pressed ? 0.98 : 1.0);
        });

        return button;
    }

    // How deep the slanted ends are: the same proportion for every height
    private static double bevel(double height)
    {
        return Math.round(height * 0.42);
    }

    // The style that gives a region the shape of an elongated hexagon of exactly this size
    private static String shape(double width, double height)
    {
        double bevel = bevel(height);

        return String.format(Locale.ROOT,
                "-fx-shape: \"M %1$.1f 0 L %2$.1f 0 L %3$.1f %4$.1f L %2$.1f %5$.1f L %1$.1f %5$.1f L 0 %4$.1f Z\";",
                bevel, width - bevel, width, height / 2, height);
    }

    private static void fixSize(Region region, double width, double height)
    {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
    }
}