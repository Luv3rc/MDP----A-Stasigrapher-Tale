package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenter;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

final class PartyPanel {

    private static final double WIDTH = 230;
    private static final double PORTRAIT_SIZE = 58;      // the face is a square of this size, without frame
    private static final double HP_Y = 65;
    private static final double HP_HEIGHT = 22;
    private static final double AP_Y = 90;
    private static final double AP_HEIGHT = 17;
    private static final double DIAMOND = 30;
    private static final double AP_GAP = 3;

    private static final String AP_COLOR = "#56b9ec";
    private static final String INK_COLOR = "#3d8fd6";

    private final BattlePresenter.FighterView view;
    private final Pane root = new Pane();

    private final Rectangle hpBackground =
            new Rectangle(WIDTH, HP_HEIGHT);
    private final Rectangle hpFill =
            new Rectangle(WIDTH, HP_HEIGHT);
    private final Rectangle hpBorder =
            new Rectangle(WIDTH, HP_HEIGHT);

    private final Label lifeLabel = new Label();
    private final Label inkLabel = new Label();
    private final Label apNumber = new Label();

    private final Rectangle inkFill = new Rectangle();
    private final HBox apRow = new HBox(AP_GAP);
    private final List<Rectangle> apBoxes = new ArrayList<>();

    private StackPane diamond;
    private StackPane portrait;

    PartyPanel(BattlePresenter.FighterView view) {
        this.view = view;

        root.setPrefSize(WIDTH, HP_Y + HP_HEIGHT + AP_HEIGHT);
        root.setMinSize(WIDTH, HP_Y + HP_HEIGHT + AP_HEIGHT);
        root.setMaxSize(WIDTH, HP_Y + HP_HEIGHT + AP_HEIGHT);

        buildPortrait();
        buildHpBar();
        buildApDiamond();

        root.getChildren().addAll(
                hpBackground,
                hpFill,
                hpBorder,
                lifeLabel,
                apRow,
                diamond
        );

        if (view.getMaxInk() > 0) {
            buildInk();
        }

        update();
    }

    Pane getRoot() {
        return root;
    }

    void update() {
        int maxHp = Math.max(1, view.getMaxHp());
        double hpRatio = Math.max(0,
                Math.min(1, (double) view.getShownHp() / maxHp));

        hpFill.setWidth(WIDTH * hpRatio);
        lifeLabel.setText(
                view.getShownHp() + " / " + view.getMaxHp());

        if (view.getMaxInk() > 0) {
            inkLabel.setText(view.getInk() + " / " + view.getMaxInk());

            double ratio = Math.max(0, Math.min(1, (double) view.getInk() / view.getMaxInk()));

            inkFill.setHeight(15 * ratio);
            inkFill.setY(26 - inkFill.getHeight());
        }

        if (apBoxes.size() != view.getMaxAp()) {
            buildAp(view.getMaxAp());
        }

        apNumber.setText(String.valueOf(view.getAp()));

        for (int i = 0; i < apBoxes.size(); i++) {
            apBoxes.get(i).setFill(Color.web(
                    i < view.getAp() ? AP_COLOR : "#171923"));
        }
    }

    // The face: a transparent square with no border. The picture is resized to fit inside it (the proportions are
    // kept, so a picture that is not square just leaves empty space on its sides)
    private void buildPortrait() {
        Image image = SpriteArt.find(view.getName(), "head");

        portrait = new StackPane();
        portrait.setAlignment(Pos.CENTER);
        portrait.setPrefSize(PORTRAIT_SIZE, PORTRAIT_SIZE);
        portrait.setMinSize(PORTRAIT_SIZE, PORTRAIT_SIZE);
        portrait.setMaxSize(PORTRAIT_SIZE, PORTRAIT_SIZE);
        portrait.setLayoutX(WIDTH - PORTRAIT_SIZE);
        portrait.setLayoutY(0);

        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(PORTRAIT_SIZE);
            imageView.setFitHeight(PORTRAIT_SIZE);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            portrait.getChildren().add(imageView);
        } else {
            // No picture yet: just the first letter, on nothing
            Label initial = new Label(
                    view.getName().substring(0, 1));
            initial.setStyle(
                    "-fx-text-fill: white;" +
                            "-fx-font-size: 34px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-effect: dropshadow(gaussian, black, 4, 0.8, 0, 0);");
            portrait.getChildren().add(initial);
        }

        root.getChildren().add(portrait);
    }

    private void buildHpBar() {
        hpBackground.setLayoutX(0);
        hpBackground.setLayoutY(HP_Y);
        hpBackground.setFill(Color.web("#24232b"));

        hpFill.setLayoutX(0);
        hpFill.setLayoutY(HP_Y);
        hpFill.setFill(Color.web("#b72f48"));

        hpBorder.setLayoutX(0);
        hpBorder.setLayoutY(HP_Y);
        hpBorder.setFill(Color.TRANSPARENT);
        hpBorder.setStroke(Color.web("#15151b"));
        hpBorder.setStrokeWidth(2);

        // Prevents the border from expanding the visual width to 232
        hpBorder.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);

        lifeLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 0 5 0 0;" +
                        "-fx-effect: dropshadow(gaussian, black, 3, 0.8, 0, 1);"
        );

        lifeLabel.setAlignment(Pos.CENTER_RIGHT);
        lifeLabel.setLayoutX(45);
        lifeLabel.setLayoutY(HP_Y);
        lifeLabel.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
    }

    private void buildInk() {
        // Bottle body
        Rectangle body = new Rectangle(2, 9, 14, 19);
        body.setArcWidth(5);
        body.setArcHeight(5);
        body.setFill(Color.web("#171923"));
        body.setStroke(Color.web("#d6d5ce"));
        body.setStrokeWidth(1.2);

        // Narrow neck
        Rectangle neck = new Rectangle(6, 3, 6, 7);
        neck.setFill(Color.web("#171923"));
        neck.setStroke(Color.web("#d6d5ce"));
        neck.setStrokeWidth(1.2);

        // Bottle opening
        Rectangle opening = new Rectangle(5, 2, 8, 2);
        opening.setFill(Color.web("#d6d5ce"));

        // Ink inside the bottle, filling from the bottom
        inkFill.setX(4);
        inkFill.setWidth(10);
        inkFill.setY(11);
        inkFill.setHeight(15);
        inkFill.setArcWidth(3);
        inkFill.setArcHeight(3);
        inkFill.setFill(Color.web(INK_COLOR));

        Pane bottle = new Pane(body, neck, opening, inkFill);
        bottle.setPrefSize(18, 30);
        bottle.setMinSize(18, 30);
        bottle.setMaxSize(18, 30);

        inkLabel.setStyle(
                "-fx-text-fill: #e5e5e5;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;"
        );

        HBox ink = new HBox(5, bottle, inkLabel);
        ink.setAlignment(Pos.CENTER_LEFT);
        ink.setLayoutX(0);
        ink.setLayoutY(HP_Y - 32);

        root.getChildren().add(ink);
    }

    private void buildAp(int count) {
        apBoxes.clear();
        apRow.getChildren().clear();

        count = Math.max(0, count);

        double startX = 20;
        double availableWidth = WIDTH - startX;
        double boxWidth = count == 0 ? 0 :
                (availableWidth - AP_GAP * (count - 1)) / count;

        for (int i = 0; i < count; i++) {
            Rectangle box = new Rectangle(boxWidth, AP_HEIGHT);
            box.setArcWidth(2);
            box.setArcHeight(2);
            box.setFill(Color.web("#171923"));
            box.setStroke(Color.web("#343540"));
            box.setStrokeWidth(0.8);
            box.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);
            apBoxes.add(box);
        }

        apRow.getChildren().addAll(apBoxes);
        apRow.setAlignment(Pos.CENTER_LEFT);
        apRow.setLayoutX(startX);
        apRow.setLayoutY(AP_Y);
        apRow.setPrefHeight(AP_HEIGHT);

        // Impedisce all'HBox di arrotondare le dimensioni decimali dei riquadri
        apRow.setSnapToPixel(false);
    }

    private void buildApDiamond() {
        Polygon shape = new Polygon(
                DIAMOND / 2, 0,
                DIAMOND, DIAMOND / 2,
                DIAMOND / 2, DIAMOND,
                0, DIAMOND / 2
        );

        shape.setFill(Color.web("#164a76"));
        shape.setStroke(Color.web("#6fc9ff"));
        shape.setStrokeWidth(1.5);

        apNumber.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;");

        StackPane textPane = new StackPane(apNumber);
        textPane.setAlignment(Pos.CENTER);

        diamond = new StackPane(shape, textPane);
        diamond.setPrefSize(DIAMOND, DIAMOND);
        diamond.setMinSize(DIAMOND, DIAMOND);
        diamond.setMaxSize(DIAMOND, DIAMOND);
        diamond.setLayoutX(0);
        diamond.setLayoutY(AP_Y + (AP_HEIGHT - DIAMOND) / 2);
    }
}