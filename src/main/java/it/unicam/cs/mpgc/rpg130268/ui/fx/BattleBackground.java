package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Objects;

/* *
    The background of the battle: the picture of the area, a bit darkened so the rest is easy to read.
    It fills the whole window, from its corner. It is outside of the layout (not managed): nothing that changes in the
    menu can move it or change its size.
    The screen adds nodes() under everything else.
* */
final class BattleBackground
{
    private static final double DIM = 0.35;       // how much the picture is darkened (0 = not at all)

    private final ImageView picture = new ImageView();
    private final Rectangle dim = new Rectangle();
    private final Region content;       // what is drawn over the background: it is see-through only if there is a picture

    BattleBackground(Region window, Region content)
    {
        Objects.requireNonNull(window, "window must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null");

        picture.setPreserveRatio(false);
        picture.setManaged(false);
        picture.setMouseTransparent(true);
        picture.fitWidthProperty().bind(window.widthProperty());
        picture.fitHeightProperty().bind(window.heightProperty());

        dim.setManaged(false);
        dim.setMouseTransparent(true);
        dim.setFill(Color.rgb(0, 0, 0, DIM));
        dim.widthProperty().bind(window.widthProperty());
        dim.heightProperty().bind(window.heightProperty());
    }

    // The picture and the dark layer over it, from the back to the front
    List<Node> nodes() { return List.of(picture, dim); }

    // The background of the area: background_<area>.png, otherwise the general background.png, otherwise a dark color
    void show(String area)
    {
        Image image = SpriteArt.background(area);

        picture.setImage(image);
        dim.setVisible(image != null);
        content.setStyle(image == null ? "-fx-background-color: #1b1b27;" : "-fx-background-color: transparent;");
    }
}