package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.reward.RewardPresenter.MemberView;
import it.unicam.cs.mpgc.rpg130268.ui.reward.RewardPresenter.StatView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.Objects;

/* *
    It is the dark panel with the gold border of the rest of the game (GoldFrame) and in it there are
    the face in a hexagon (the head.png of the character, as in the HUD of the battle) and the name, the big XP bar
    under the name with the XP earned of the next level and the XP of the battle, "Level Up!" if the level went up, the
    level in big numbers, the stats each one as a bar with its number and, at the bottom, the points that the member
    has to spend at a campfire
* */
final class MemberRewardPanel
{
    static final double WIDTH = 380;
    static final double HEIGHT = 214;

    private static final String SERIF = GoldFrame.SERIF;
    private static final double TEXT_LEFT = 150;                // where the name, the bars and the stats start
    private static final double TEXT_RIGHT = 50;                // the space left on the right (the border is slanted)
    private static final double XP_BAR_HEIGHT = 12;
    private static final double STAT_BAR_WIDTH = 80;
    private static final double STAT_BAR_HEIGHT = 7;
    private static final double STAT_ROW = 19;

    private final Pane root = new Pane();

    MemberRewardPanel(MemberView member)
    {
        Objects.requireNonNull(member, "member must not be null");

        root.setMinSize(WIDTH, HEIGHT);
        root.setPrefSize(WIDTH, HEIGHT);
        root.setMaxSize(WIDTH, HEIGHT);
        root.getChildren().addAll(GoldFrame.create(WIDTH, HEIGHT), portrait(member));

        add(text(member.name().toUpperCase(), SERIF + "-fx-text-fill: #f5ebd0; -fx-font-size: 21px;"), TEXT_LEFT, 14);

        if (member.leveledUp())
        {
            Label levelUp = text("Level Up!", SERIF + "-fx-text-fill: #e8d08a; -fx-font-size: 15px; -fx-font-style: italic;");
            levelUp.setPrefWidth(110);
            levelUp.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            add(levelUp, WIDTH - TEXT_RIGHT - 110, 18);
        }

        String xpText = member.xpForNext() == 0 ? "MAX" : member.xp() + " / " + member.xpForNext();
        add(text(xpText, "-fx-text-fill: #9a9aab; -fx-font-size: 11px;"), TEXT_LEFT, 46);

        Label gained = text("+" + member.xpGained() + " XP", "-fx-text-fill: #e8d08a; -fx-font-size: 11px; -fx-font-weight: bold;");
        gained.setPrefWidth(110);
        gained.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        add(gained, WIDTH - TEXT_RIGHT - 110, 46);
        add(bar(WIDTH - TEXT_LEFT - TEXT_RIGHT, XP_BAR_HEIGHT, member.xpFraction(), xpFill()), TEXT_LEFT, 62);

        // The level, in big numbers, under the face
        add(text("Level", "-fx-text-fill: #9a9aab; -fx-font-size: 11px;"), 62, 118);
        add(text(String.valueOf(member.level()), SERIF + "-fx-text-fill: #f5ebd0; -fx-font-size: 40px;"), 58, 128);

        double y = 84;
        for (StatView stat : member.stats())
        {
            addStat(stat, y);
            y += STAT_ROW;
        }

        if (member.unspentPoints() > 0)
        {
            String points = member.unspentPoints() == 1 ? "1 point to spend" : member.unspentPoints() + " points to spend";
            add(text(points, SERIF + "-fx-text-fill: #e8d08a; -fx-font-size: 14px; -fx-font-style: italic;"), TEXT_LEFT, y + 6);
        }
    }

    Node getRoot() { return root; }


    // ---------- Inside ----------

    private void addStat(StatView stat, double y)
    {
        add(text(stat.label(), "-fx-text-fill: #b9b9c6; -fx-font-size: 12px;"), TEXT_LEFT, y);
        add(bar(STAT_BAR_WIDTH, STAT_BAR_HEIGHT, stat.fraction(), Color.web("#b8a46a")), TEXT_LEFT + 72, y + 5);

        Label value = text(String.valueOf(stat.value()), "-fx-text-fill: #f5ebd0; -fx-font-size: 13px; -fx-font-weight: bold;");
        value.setPrefWidth(32);
        value.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        add(value, TEXT_LEFT + 72 + STAT_BAR_WIDTH + 2, y);
    }

    private static Node portrait(MemberView member)
    {
        double radius = 46;
        double centerX = 90;
        double centerY = 70;

        Polygon back = hexagon(centerX, centerY, radius);
        back.setFill(Color.web("#171923"));
        back.setStroke(Color.web("#d2b45a"));
        back.setStrokeWidth(2.5);

        Pane portrait = new Pane(back);
        Image head = SpriteArt.find(member.artName(), "head");

        if (head != null)
        {
            ImageView picture = new ImageView(head);
            picture.setPreserveRatio(true);
            picture.setSmooth(false);
            picture.setFitWidth(radius * 1.7);
            picture.setFitHeight(radius * 1.7);
            picture.setLayoutX(centerX - radius * 0.85);
            picture.setLayoutY(centerY - radius * 0.85);
            picture.setClip(hexagon(radius * 0.85, radius * 0.85, radius * 0.85));
            portrait.getChildren().add(picture);
        }
        else
        {
            Label initial = new Label(member.name().substring(0, 1).toUpperCase());
            initial.setStyle(SERIF + "-fx-text-fill: #d2b45a; -fx-font-size: 42px;");
            initial.setLayoutX(centerX - 14);
            initial.setLayoutY(centerY - 30);
            portrait.getChildren().add(initial);
        }

        return portrait;
    }

    private static Polygon hexagon(double centerX, double centerY, double radius)
    {
        Polygon hexagon = new Polygon();

        for (int corner = 0; corner < 6; corner++)
        {
            double angle = Math.toRadians(60 * corner - 90);
            hexagon.getPoints().addAll(centerX + radius * Math.cos(angle), centerY + radius * Math.sin(angle));
        }

        return hexagon;
    }

    private static Node bar(double width, double height, double fraction, javafx.scene.paint.Paint fill)
    {
        Rectangle track = new Rectangle(width, height);
        track.setFill(Color.web("#24232b"));
        track.setStroke(Color.web("#15151b"));

        Rectangle full = new Rectangle(1, 1, Math.max(0, (width - 2) * fraction), height - 2);
        full.setFill(fill);

        Pane bar = new Pane(track, full);
        bar.setMinSize(width, height);
        bar.setPrefSize(width, height);
        bar.setMaxSize(width, height);

        return bar;
    }

    private static javafx.scene.paint.Paint xpFill()
    {
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f5e1a0")), new Stop(1, Color.web("#b58f3e")));
    }

    private static Label text(String text, String style)
    {
        Label label = new Label(text);
        label.setStyle(style);
        return label;
    }

    private void add(Node node, double x, double y)
    {
        node.setLayoutX(x);
        node.setLayoutY(y);
        root.getChildren().add(node);
    }
}
