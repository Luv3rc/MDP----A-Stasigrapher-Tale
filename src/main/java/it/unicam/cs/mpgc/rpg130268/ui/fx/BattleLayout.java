package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.Cue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

final class BattleLayout
{
    private static final double HUD_MARGIN = 14;
    private static final double BOTTOM_MARGIN = 40;
    private static final double PARTY_LEFT = 300;
    private static final double MENU_LEFT = PARTY_LEFT + 150 + 36;
    private static final double ENEMIES_RIGHT = 120;
    private static final double ENEMIES_BOTTOM = 150;

    private final BorderPane root = new BorderPane();
    private final Pane slashLayer = new Pane();
    private final StackPane stage = new StackPane(root, slashLayer);

    private final StackPane field = new StackPane();
    private final HBox partyBox = new HBox(24);
    private final HBox hudBox = new HBox(16);
    private final Label titleLabel = new Label();
    private final Label lockLabel = new Label();
    private final Label hintLabel = new Label();
    private final TimingBar timingBar = new TimingBar();
    private final TextArea logArea = new TextArea();

    private boolean timingBarOn = false;

    BattleLayout(Region enemies, Region menu, BattleOverlay overlay)
    {
        // The cuts go over everything but they must not catch the mouse, and they never draw outside of the window
        slashLayer.setMouseTransparent(true);
        Rectangle slashClip = new Rectangle();
        slashClip.widthProperty().bind(slashLayer.widthProperty());
        slashClip.heightProperty().bind(slashLayer.heightProperty());
        slashLayer.setClip(slashClip);

        root.setMinSize(0, 0);

        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        lockLabel.setStyle("-fx-text-fill: #e67e22; -fx-font-size: 16px; -fx-font-weight: bold;");
        Region topSpace = new Region();
        HBox.setHgrow(topSpace, Priority.ALWAYS);
        HBox titleRow = new HBox(12, titleLabel, topSpace, lockLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label legend = new Label("green = parry window    blue = dodge window    white line = impact");
        legend.setStyle("-fx-text-fill: #8888aa; -fx-font-size: 12px;");
        legend.visibleProperty().bind(timingBar.visibleProperty());

        VBox top = new VBox(6, titleRow, timingBar, legend);
        top.setAlignment(Pos.TOP_CENTER);
        top.setPadding(new Insets(12, 14, 0, 14));
        root.setTop(top);


        // Center: the field.
        // Gunther on the left and the enemies on the right, facing each other
        // Menu's on right of Gunther, and the HUD in the top-right corner
        partyBox.setAlignment(Pos.BOTTOM_CENTER);
        hudBox.setAlignment(Pos.TOP_RIGHT);
        hudBox.setSpacing(12);
        for (HBox box : List.of(partyBox, hudBox)) box.setFillHeight(false);
        for (Region box : List.of(enemies, partyBox, hudBox, menu)) box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(enemies, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(enemies, new Insets(0, ENEMIES_RIGHT, ENEMIES_BOTTOM, 0));
        StackPane.setAlignment(partyBox, Pos.BOTTOM_LEFT);
        StackPane.setMargin(partyBox, new Insets(0, 0, BOTTOM_MARGIN, PARTY_LEFT));
        StackPane.setAlignment(menu, Pos.BOTTOM_LEFT);
        StackPane.setMargin(menu, new Insets(0, 0, BOTTOM_MARGIN, MENU_LEFT));
        StackPane.setAlignment(hudBox, Pos.TOP_RIGHT);
        StackPane.setMargin(hudBox, new Insets(HUD_MARGIN, HUD_MARGIN, 0, 0));

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setFocusTraversable(false);
        logArea.setStyle("-fx-control-inner-background: #14141d; -fx-text-fill: #dddddd; -fx-opacity: 0.92;");
        logArea.setMaxSize(360, 220);
        logArea.setVisible(false);
        StackPane.setAlignment(logArea, Pos.TOP_LEFT);
        StackPane.setMargin(logArea, new Insets(8));

        hintLabel.setStyle("-fx-text-fill: #ccccdd; -fx-font-size: 14px; -fx-font-weight: bold; "
                + "-fx-effect: dropshadow(gaussian, black, 4, 0.8, 0, 0);");
        hintLabel.setMouseTransparent(true);
        StackPane.setAlignment(hintLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(hintLabel, new Insets(0, 0, 8, 10));

        field.getChildren().addAll(enemies, partyBox, menu, hintLabel, logArea, overlay.bannerNode());

        // The announcement and the HUD are over the WHOLE window
        stage.getChildren().add(stage.getChildren().indexOf(slashLayer), overlay.announcementNode());
        stage.getChildren().add(stage.getChildren().indexOf(slashLayer), hudBox);
        root.setCenter(field);
    }


    // ---------- Where it is ----------

    Region stage() { return stage; }

    Region content() { return root; }

    Pane slashLayer() { return slashLayer; }

    void addStylesheet(URL stylesheet) { stage.getStylesheets().add(stylesheet.toExternalForm()); }

    void addBehind(Collection<? extends Node> nodes) { stage.getChildren().addAll(0, nodes); }

    void addOnTop(Node node) { stage.getChildren().add(node); }


    // ---------- The fighters ----------

    void clearFighters()
    {
        partyBox.getChildren().clear();
        hudBox.getChildren().clear();
    }

    void addPartyMember(Node card, Node hud)
    {
        partyBox.getChildren().add(card);
        hudBox.getChildren().add(hud);
    }


    // ---------- What it says ----------

    void setTitle(String text) { titleLabel.setText(text); }

    void setLock(String text) { lockLabel.setText(text); }

    void setHint(String text) { hintLabel.setText(text); }

    void toggleTimingBar() { timingBarOn = !timingBarOn; }

    void showCue(Optional<Cue> cue)
    {
        if (timingBarOn)
            cue.ifPresentOrElse(timingBar::show, timingBar::hide);
        else
            timingBar.hide();
    }

    void toggleLog() { logArea.setVisible(!logArea.isVisible()); }

    void appendLog(String line) { logArea.appendText(line + "\n"); }

    void clearLog() { logArea.clear(); }
}
