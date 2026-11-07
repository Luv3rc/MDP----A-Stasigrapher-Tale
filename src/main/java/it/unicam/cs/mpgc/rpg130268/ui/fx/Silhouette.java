package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;

// A character that has no picture yet, drawn as a head over a pair of shoulders in one color. Just for placeholding
final class Silhouette
{
    private Silhouette() { }

    static Node of(double width, double height, Color color)
    {
        Ellipse shoulders = new Ellipse(width / 2, height * 0.78, width * 0.34, height * 0.3);
        shoulders.setFill(color);

        Circle head = new Circle(width / 2, height * 0.3, width * 0.17);
        head.setFill(color.brighter());

        Pane pane = new Pane(shoulders, head);
        pane.setMinSize(width, height);
        pane.setPrefSize(width, height);
        pane.setMaxSize(width, height);

        return pane;
    }
}
