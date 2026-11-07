package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;

/* *
    The dark panel with the gold border, drawn with shapes. It is the frame of the questions (QuestionPopup)
    and of the box of the dialogues, which is the same panel stretched

     The slanted ends always have the same depth (BEVEL), so the panel can be as wide as needed.
* */
final class GoldFrame
{
    static final double BEVEL = 38;                       // how deep the slanted ends are
    static final String SERIF = "-fx-font-family: 'Palatino Linotype', 'Book Antiqua', Georgia, serif;";

    private static final double BORDER = 3;               // the thickness of the gold border
    private static final double LINE_INSET = 12;          // where the thin line is

    private GoldFrame() { }

    static Pane create(double width, double height)
    {
        Polygon gold = hexagon(0, width, height);
        gold.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f0d278")), new Stop(0.5, Color.web("#785f28")), new Stop(1, Color.web("#d2b45a"))));

        Polygon dark = hexagon(BORDER, width, height);
        dark.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.7, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#24272d")), new Stop(1, Color.web("#0d0e11"))));

        Polygon line = hexagon(LINE_INSET, width, height);
        line.setFill(Color.TRANSPARENT);
        line.setStroke(Color.web("#d2b45a", 0.55));
        line.setStrokeWidth(1);

        Pane frame = new Pane(gold, dark, line,
                diamond(BEVEL * 0.5 + 2, height / 2), diamond(width - BEVEL * 0.5 - 2, height / 2));
        frame.setMinSize(width, height);
        frame.setPrefSize(width, height);
        frame.setMaxSize(width, height);

        return frame;
    }

    private static Polygon hexagon(double inset, double width, double height)
    {
        // The slanted ends keep their angle, so the bevel grows a little with the inset
        double b = BEVEL + inset * 0.42;

        return new Polygon(
                b, inset,
                width - b, inset,
                width - inset, height / 2,
                width - b, height - inset,
                b, height - inset,
                inset, height / 2);
    }

    private static Polygon diamond(double centerX, double centerY)
    {
        double r = 5;
        Polygon diamond = new Polygon(centerX, centerY - r, centerX + r, centerY, centerX, centerY + r, centerX - r, centerY);
        diamond.setFill(Color.web("#d2b45a"));

        return diamond;
    }
}
