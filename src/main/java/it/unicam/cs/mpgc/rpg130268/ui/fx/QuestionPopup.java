package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.question.Choice;
import it.unicam.cs.mpgc.rpg130268.ui.question.QuestionView;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.util.Objects;

/* *
    A question on top of a screen, like "Return to desktop?" with its two buttons. The screen is darkened and in
    the middle there is the dark panel with the gold border (GoldFrame)

    It shows the QuestionView it is given and tells the screen when a button is clicked
    While it is on view it covers the whole screen and catches the mouse, so what is behind cannot be used
* */
final class QuestionPopup
{
    private static final double WIDTH = 620;
    private static final double HEIGHT = 230;
    private static final double BEVEL = GoldFrame.BEVEL;
    private static final double LINE_INSET = 12;          // where the thin line of the frame is
    private static final String SERIF = GoldFrame.SERIF;

    private final StackPane root = new StackPane();
    private final Label title = new Label();
    private final Label message = new Label();
    private final Button confirm;
    private final Button cancel;

    QuestionPopup(RpgButtons buttons, Runnable onConfirm, Runnable onCancel)
    {
        Objects.requireNonNull(buttons, "buttons must not be null");
        Objects.requireNonNull(onConfirm, "onConfirm must not be null");
        Objects.requireNonNull(onCancel, "onCancel must not be null");

        confirm = buttons.compact("Confirm");
        cancel = buttons.compact("Cancel");
        confirm.setOnAction(event -> onConfirm.run());
        cancel.setOnAction(event -> onCancel.run());

        title.setStyle(SERIF + "-fx-text-fill: #f5ebd0; -fx-font-size: 26px;");
        message.setStyle(SERIF + "-fx-text-fill: #b8b8cc; -fx-font-size: 15px;");
        message.setWrapText(true);
        message.setTextAlignment(TextAlignment.CENTER);
        message.setAlignment(Pos.CENTER);
        message.setMaxWidth(WIDTH - 2 * BEVEL - 60);

        HBox choices = new HBox(32, confirm, cancel);
        choices.setAlignment(Pos.CENTER);

        VBox content = new VBox(18, title, message, choices);
        content.setAlignment(Pos.CENTER);
        content.setMaxSize(WIDTH - 2 * BEVEL, HEIGHT - 2 * LINE_INSET);

        StackPane panel = new StackPane(GoldFrame.create(WIDTH, HEIGHT), content);
        panel.setMaxSize(WIDTH, HEIGHT);
        panel.setEffect(new DropShadow(24, 0, 6, Color.rgb(0, 0, 0, 0.8)));

        // The dark veil covers everything and takes the mouse
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        root.getChildren().add(panel);
        root.setVisible(false);
    }

    StackPane getRoot() { return root; }

    void show(QuestionView question)
    {
        title.setText(question.title().toUpperCase());
        message.setText(question.message());
        message.setVisible(!question.message().isBlank());
        message.setManaged(!question.message().isBlank());

        RpgButtons.setText(confirm, question.confirmLabel());
        RpgButtons.setText(cancel, question.cancelLabel());

        cancel.setVisible(question.hasCancel());
        cancel.setManaged(question.hasCancel());

        GameStyle.setSelected(confirm, question.selected() == Choice.CONFIRM);
        GameStyle.setSelected(cancel, question.selected() == Choice.CANCEL);

        root.setVisible(true);
    }

    void hide() { root.setVisible(false); }

    boolean isShown() { return root.isVisible(); }
}
