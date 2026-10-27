package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenter;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/* *
    How a fighter looks on the battlefield: a sprite, and the numbers that float over it.
    - A party member is seen from BEHIND (just the sprite: its life, AP and ink are in the HUD, see PartyPanel)
    - An enemy is seen from the front, with its name and a small life bar over the sprite
    The picture comes from the resources (see SpriteArt); without it a placeholder silhouette is drawn.
    It only draws what the presenter's FighterView says (the bar follows what has been SHOWN)
* */
final class FighterCard
{
    private static final String PARTY_COLOR = "#4f6fa8";
    private static final String ENEMY_COLOR = "#8a4f5a";

    private static final int FLOAT_MS = 2400;      // how long a number stays over the sprite
    private static final int FADE_MS = 700;        // the last part of it, while it fades away

    private final BattlePresenter.FighterView view;
    private final Node sprite;
    private final StackPane root = new StackPane();
    private final Label nameLabel = new Label();
    private final Label hpLabel = new Label();
    private final ProgressBar hpBar = new ProgressBar(1.0);
    private final Pane overlay = new Pane();

    private String glow = "?";

    FighterCard(BattlePresenter.FighterView view)
    {
        this.view = view;

        boolean party = view.isPartyMember();
        sprite = buildSprite(party ? 150 : 110, party ? 260 : 192);

        VBox box = new VBox(4);
        box.setAlignment(Pos.TOP_CENTER);

        if (!party)
        {
            nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; "
                    + "-fx-effect: dropshadow(gaussian, black, 4, 0.8, 0, 0);");
            hpLabel.setStyle("-fx-text-fill: #dddddd; -fx-font-size: 12px; "
                    + "-fx-effect: dropshadow(gaussian, black, 4, 0.8, 0, 0);");
            hpBar.setPrefWidth(110);
            box.getChildren().addAll(nameLabel, hpBar, hpLabel);
        }

        box.getChildren().add(sprite);

        // The floating numbers must not catch the mouse: the sprite can be clicked to choose it as a target
        overlay.setMouseTransparent(true);

        root.getChildren().addAll(box, overlay);
        root.setPrefWidth(party ? 150 : 130);
        root.setMinWidth(party ? 150 : 130);

        update(false, false, false);
    }

    StackPane getRoot() { return root; }

    // What the cuts aim at: the sprite itself (the middle of the card also holds the name and the bar)
    Node getTarget() { return sprite; }

    BattlePresenter.FighterView getView() { return view; }

    // active: it is its turn. targetable: the player can choose it now. selected: it is the one chosen right now
    void update(boolean active, boolean targetable, boolean selected)
    {
        if (!view.isPartyMember())
        {
            nameLabel.setText(view.getName());
            hpLabel.setText(view.getShownHp() + " / " + view.getMaxHp());
            hpBar.setProgress((double) view.getShownHp() / view.getMaxHp());
        }

        root.setOpacity(view.isAlive() ? 1.0 : 0.35);
        root.setCursor(targetable ? Cursor.HAND : Cursor.DEFAULT);

        // The selected target gets a small red glow (radius 12, spread 0.5), other targets don't glow at all.
        // Active characters still get their yellow turn indicator.
        String newGlow = selected ? "#e74c3c,12,0.5" : active ? "#f1c40f,24,0.6" : "";

        if (!newGlow.equals(glow))
        {
            glow = newGlow;

            if (glow.isEmpty())
                root.setStyle("");
            else
            {
                String[] parts = glow.split(",");
                root.setStyle("-fx-effect: dropshadow(gaussian, " + parts[0] + ", " + parts[1] + ", " + parts[2] + ", 0, 0);");
            }
        }
    }

    // A number (or a word) that rises slowly over the sprite, stays readable for most of FLOAT_MS and fades at the end.
    // The ones that are on the screen together do not sit on top of each other: every new one starts a bit higher
    // and on the other side
    void floatText(String text, String color, int size)
    {
        int others = overlay.getChildren().size();

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "px; -fx-font-weight: bold; "
                + "-fx-effect: dropshadow(gaussian, black, 4, 0.8, 0, 0);");
        label.setLayoutX(24 + (others % 2 == 0 ? 0 : 34));
        label.setLayoutY((view.isPartyMember() ? 30 : 60) - 24 * (others % 3));
        overlay.getChildren().add(label);

        TranslateTransition rise = new TranslateTransition(Duration.millis(FLOAT_MS), label);
        rise.setByY(-40);

        FadeTransition fade = new FadeTransition(Duration.millis(FADE_MS), label);
        fade.setDelay(Duration.millis(FLOAT_MS - FADE_MS));
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        ParallelTransition animation = new ParallelTransition(rise, fade);
        animation.setOnFinished(event -> overlay.getChildren().remove(label));
        animation.play();
    }

    // A quick step to the side and back: it dodged
    void sidestep()
    {
        TranslateTransition step = new TranslateTransition(Duration.millis(110), root);
        step.setByX(view.isPartyMember() ? -36 : 36);
        step.setCycleCount(2);
        step.setAutoReverse(true);
        step.play();
    }

    // A short shake: it got hit
    void shake()
    {
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), root);
        shake.setByX(7);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.play();
    }

    // ---------- Inside ----------

    // The picture if there is one, otherwise a silhouette: a head over a pair of shoulders (eyes if it faces us)
    private Node buildSprite(double width, double height)
    {
        boolean party = view.isPartyMember();
        Image image = SpriteArt.find(view.getName(), party ? "back" : "front");

        if (image != null)
        {
            ImageView picture = new ImageView(image);
            picture.setFitWidth(width);
            picture.setFitHeight(height);
            picture.setPreserveRatio(true);
            return picture;
        }

        Color color = Color.web(party ? PARTY_COLOR : ENEMY_COLOR);

        Ellipse shoulders = new Ellipse(width / 2, height * 0.72, width * 0.48, height * 0.42);
        shoulders.setFill(color);

        Circle head = new Circle(width / 2, width * 0.27, width * 0.24);
        head.setFill(color.brighter());

        Pane sprite = new Pane(shoulders, head);
        sprite.setPrefSize(width, height);
        sprite.setMinSize(width, height);
        sprite.setMaxSize(width, height);
        sprite.setClip(new Rectangle(width, height));

        if (!party)
        {
            for (double side : new double[] {-1, 1})
            {
                Circle eye = new Circle(width / 2 + side * width * 0.09, width * 0.25, width * 0.035);
                eye.setFill(Color.WHITE);
                sprite.getChildren().add(eye);
            }
        }

        return sprite;
    }
}