package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.reward.RewardPresenter;
import it.unicam.cs.mpgc.rpg130268.ui.reward.RewardPresenter.MemberView;
import it.unicam.cs.mpgc.rpg130268.ui.reward.RewardPresenter.View;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;

import java.util.Objects;

/* *
    On the left there is the big gold "VICTORY" and, next to it, the XP of the battle. On the right there is a panel
    for every party member (MemberRewardPanel) with their faces, name, XP bar, level, stats and the points they have to
    spend. The background is the one of the battle, darkened like in the dialogues

    The XP bars are shown at their end value (their animation will be done later maybe)
* */
final class RewardScreen implements GameScreen
{
    private static final double BACKGROUND_DARKNESS = 0.6;
    private static final long INPUT_LOCK_NANOS = 700_000_000L;
    private static final double PANELS_LEFT = SceneHost.DESIGN_WIDTH - MemberRewardPanel.WIDTH - 20;
    private static final double PANELS_TOP = 40;
    private static final double PANELS_GAP = 10;
    private static final String SERIF = GoldFrame.SERIF;

    private final Runnable onContinue;
    private final Pane root = new Pane();

    private long shownNanos;
    private boolean ended = false;

    RewardScreen(RewardPresenter presenter, String area, Runnable onContinue)
    {
        Objects.requireNonNull(presenter, "presenter must not be null");
        Objects.requireNonNull(area, "area must not be null");
        this.onContinue = Objects.requireNonNull(onContinue, "onContinue must not be null");

        View view = presenter.view();

        BattleBackground background = new BattleBackground(root, root, BACKGROUND_DARKNESS);
        root.getChildren().addAll(background.nodes());
        background.show(area);

        root.getChildren().addAll(scrim(), title(view.title()), underline(), battleXp(view.totalXp()), panels(view), hint());

        root.setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.PRIMARY)
                next();
        });
    }

    @Override
    public Region getRoot() { return root; }

    @Override
    public void onShow() { shownNanos = System.nanoTime(); }

    @Override
    public void onKeyPressed(KeyEvent event)
    {
        switch (event.getCode())
        {
            case ENTER, SPACE -> next();
            default -> { }
        }

        event.consume();
    }


    // ---------- Inside ----------

    private void next()
    {
        if (ended || System.nanoTime() - shownNanos < INPUT_LOCK_NANOS)
            return;

        ended = true;
        onContinue.run();
    }

    // A shade on the left, darker at the edge, behind the title and the XP so they can be read on any picture
    private Region scrim()
    {
        Region shade = new Region();
        shade.setPrefSize(SceneHost.DESIGN_WIDTH * 0.6, SceneHost.DESIGN_HEIGHT);
        shade.setStyle("-fx-background-color: linear-gradient(to right, rgba(0, 0, 0, 0.6), rgba(0, 0, 0, 0));");
        shade.setMouseTransparent(true);

        return shade;
    }

    // The big gold word
    private Text title(String word)
    {
        Text title = new Text(word);
        title.setStyle(SERIF + "-fx-font-size: 96px;");
        title.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f7e6a8")), new Stop(1, Color.web("#a8802f"))));
        title.setEffect(new DropShadow(18, 0, 4, Color.rgb(0, 0, 0, 0.9)));
        title.setLayoutX(70);
        title.setLayoutY(130);                      // the baseline of a Text is its own y
        title.setMouseTransparent(true);

        return title;
    }

    // A thin gold line under it
    private Pane underline()
    {
        Line line = new Line(74, 156, 470, 156);
        line.setStroke(Color.web("#d2b45a", 0.7));
        line.setStrokeWidth(1.5);

        Polygon diamond = new Polygon(470, 150, 476, 156, 470, 162, 464, 156);
        diamond.setFill(Color.web("#d2b45a"));

        Pane decoration = new Pane(line, diamond);
        decoration.setMouseTransparent(true);

        return decoration;
    }

    private HBox battleXp(String amount)
    {
        Label number = new Label(amount);
        number.setStyle(SERIF + "-fx-text-fill: #f0dfae; -fx-font-size: 72px;");
        number.setEffect(new DropShadow(14, 0, 3, Color.rgb(0, 0, 0, 0.9)));

        Label unit = new Label("XP");
        unit.setStyle(SERIF + "-fx-text-fill: #b9b19a; -fx-font-size: 32px;");

        HBox xp = new HBox(10, number, unit);
        xp.setAlignment(Pos.BASELINE_LEFT);
        xp.setLayoutX(80);
        xp.setLayoutY(200);
        xp.setMouseTransparent(true);

        return xp;
    }

    // For every member
    private VBox panels(View view)
    {
        VBox column = new VBox(PANELS_GAP);

        for (MemberView member : view.members())
            column.getChildren().add(new MemberRewardPanel(member).getRoot());

        column.setLayoutX(PANELS_LEFT);
        column.setLayoutY(PANELS_TOP);
        column.setPickOnBounds(false);

        return column;
    }

    private HBox hint()
    {
        Label key = new Label("ENTER");
        key.setStyle("-fx-text-fill: #f5ebd0; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 3 10 3 10; "
                + "-fx-border-color: #d2b45a; -fx-border-radius: 12; -fx-background-color: rgba(0, 0, 0, 0.55); "
                + "-fx-background-radius: 12;");

        Label text = new Label("We continue");
        text.setStyle(SERIF + "-fx-text-fill: #f5ebd0; -fx-font-size: 20px;");

        HBox hint = new HBox(12, key, text);
        hint.setAlignment(Pos.CENTER_RIGHT);
        hint.setPrefWidth(MemberRewardPanel.WIDTH);
        hint.setLayoutX(PANELS_LEFT);
        hint.setLayoutY(SceneHost.DESIGN_HEIGHT - 60);
        hint.setMouseTransparent(true);

        return hint;
    }
}
