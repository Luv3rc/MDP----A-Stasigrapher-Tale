package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

import java.util.List;

/* *
    The enemies in formation, like on a stage: a back row and, in front of it and lower, a second row that stands
    in the gaps of the first. The first enemies fill the back row, the others the front row:
        1 enemy:   o                2 enemies:  o   o               3 enemies:  o   o
                                                                                  o
    The enemies of the front row are in the middle of two of the back row, and lower (DEPTH).
    The cards are not moved by the layout but by a holder, so their own animations (the shake of a hit)
    keep working on their own.
    The screen decides where the whole formation is on the field: here there is only how it is made.
* */
final class EnemyFormation
{
    private static final double SPACING = 190;         // between two enemies of the same row
    private static final double DEPTH = 60;            // how much lower the enemies of the front row are
    private static final double CARD_WIDTH = 130;
    private static final double CARD_HEIGHT = 252;     // sprite, name and life bar

    private final StackPane field = new StackPane();

    EnemyFormation()
    {
        field.setAlignment(Pos.BOTTOM_CENTER);
    }

    StackPane getRoot() { return field; }

    void clear() { field.getChildren().clear(); }

    void place(List<FighterCard> enemyCards)
    {
        int count = enemyCards.size();
        int back = (count + 2) / 2;                     // half of them, rounded up (and the front row fits in the gaps)
        int front = count - back;

        field.setPrefSize((back - 1) * SPACING + CARD_WIDTH, CARD_HEIGHT + (front > 0 ? DEPTH : 0));

        for (int i = 0; i < count; i++)
        {
            boolean inBackRow = i < back;
            int index = inBackRow ? i : i - back;
            int rowSize = inBackRow ? back : front;

            StackPane holder = new StackPane(enemyCards.get(i).getRoot());
            holder.setAlignment(Pos.BOTTOM_CENTER);
            holder.setTranslateX((index - (rowSize - 1) / 2.0) * SPACING);
            holder.setTranslateY(inBackRow && front > 0 ? -DEPTH : 0);

            field.getChildren().add(holder);
        }
    }
}