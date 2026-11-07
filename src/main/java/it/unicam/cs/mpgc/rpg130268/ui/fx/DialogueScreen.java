package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.game.StoryStep.DialogueStep;
import it.unicam.cs.mpgc.rpg130268.ui.dialogue.DialoguePresenter;
import it.unicam.cs.mpgc.rpg130268.ui.dialogue.DialoguePresenter.Result;
import it.unicam.cs.mpgc.rpg130268.ui.dialogue.DialoguePresenter.View;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* *
    A dialogue, with the protagonist on the left, who he talks with on the right, and under them the box with the name
    of who speaks and the text. It draws what the DialoguePresenter says and tells it the time and the "next", and it
    has no logic of its own

    The background of the scene is darkened so the dialogue is easy to follow. The characters stand over the box.
    They use their pictures if there are any (sprites/characters/NAME/idle.png, which look to the right, so the one on
    the right is mirrored) and a silhouette if there are not. The box is the panel of the questions (GoldFrame)
    stretched, with the name at the top left, empty for a narration

    See DialoguePresenter for how the text is written and what "next" does

    The keys are Enter and Space, and the mouse is the left button. There is a pause of a moment between two "next"
    because a pressed key repeats and a double click would skip a line
* */
final class DialogueScreen implements GameScreen
{
    private static final double BOX_WIDTH = 940;
    private static final double BOX_HEIGHT = 170;
    private static final double BOX_BOTTOM_MARGIN = 24;
    private static final double TEXT_INSET = GoldFrame.BEVEL + 34;        // from the side of the box
    private static final double CHARACTER = 300;                          // the box of a character (a square)
    private static final double FEET = 545;                               // the characters stand here (a bit over the text box)
    private static final double SIDE_MARGIN = 90;
    private static final double BACKGROUND_DARKNESS = 0.6;
    private static final long NEXT_PAUSE_NANOS = 180_000_000L;

    private static final String SERIF = GoldFrame.SERIF;
    private static final Color PROTAGONIST_COLOR = Color.web("#3c4a66");
    private static final Color PARTNER_COLOR = Color.web("#6a3a3a");

    private final DialoguePresenter presenter;
    private final Runnable onFinished;

    private final Pane root = new Pane();
    private final Label name = new Label();
    private final Text written = new Text();
    private final Text unwritten = new Text();
    private final Polygon arrow = new Polygon(0, 0, 18, 0, 9, 13);
    private final TranslateTransition bob = new TranslateTransition(Duration.millis(550), arrow);
    private final List<AnimatedSprite> sprites = new ArrayList<>();
    private final AnimationTimer clock;

    private long lastFrame = 0;
    private long lastNext = Long.MIN_VALUE / 2;
    private boolean ended = false;

    DialogueScreen(String protagonist, DialogueStep step, Runnable onFinished)
    {
        Objects.requireNonNull(protagonist, "protagonist must not be null");
        Objects.requireNonNull(step, "step must not be null");
        this.onFinished = Objects.requireNonNull(onFinished, "onFinished must not be null");
        this.presenter = new DialoguePresenter(step.lines());

        BattleBackground background = new BattleBackground(root, root, BACKGROUND_DARKNESS);
        root.getChildren().addAll(background.nodes());
        background.show(step.area());

        placeCharacter(character(protagonist, false, PROTAGONIST_COLOR), SIDE_MARGIN);

        if (!step.partner().isBlank())
            placeCharacter(character(step.partner(), true, PARTNER_COLOR), SceneHost.DESIGN_WIDTH - SIDE_MARGIN - CHARACTER);

        root.getChildren().add(buildBox());

        root.setOnMouseClicked(event ->
        {
            if (event.getButton() == MouseButton.PRIMARY)
                next();
        });

        clock = new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
                if (lastFrame != 0)
                {
                    long ms = (now - lastFrame) / 1_000_000L;
                    lastFrame += ms * 1_000_000L;
                    presenter.tick(ms);
                }
                else
                    lastFrame = now;

                render();
            }
        };

        render();
    }

    @Override
    public Region getRoot() { return root; }

    @Override
    public void onShow() { clock.start(); }

    @Override
    public void stop()
    {
        clock.stop();
        bob.stop();
        sprites.forEach(AnimatedSprite::stop);
    }

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
        long now = System.nanoTime();

        if (ended || now - lastNext < NEXT_PAUSE_NANOS)
            return;

        lastNext = now;

        if (presenter.advance() == Result.FINISHED)
        {
            ended = true;
            stop();
            onFinished.run();
            return;
        }

        render();
    }

    // Draws the name, the part of the text that is written and the triangle, from what the presenter says
    private void render()
    {
        View view = presenter.view();

        name.setText(view.speaker());
        name.setVisible(!view.isNarration());

        written.setText(view.written());
        unwritten.setText(view.text().substring(view.written().length()));

        arrow.setVisible(view.lineComplete());

        if (view.lineComplete() && bob.getStatus() != Animation.Status.RUNNING)
            bob.play();
        else if (!view.lineComplete())
            bob.stop();
    }

    private Node buildBox()
    {
        Pane box = new Pane(GoldFrame.create(BOX_WIDTH, BOX_HEIGHT));
        box.setMinSize(BOX_WIDTH, BOX_HEIGHT);
        box.setPrefSize(BOX_WIDTH, BOX_HEIGHT);
        box.setMaxSize(BOX_WIDTH, BOX_HEIGHT);
        box.setLayoutX((SceneHost.DESIGN_WIDTH - BOX_WIDTH) / 2);
        box.setLayoutY(SceneHost.DESIGN_HEIGHT - BOX_BOTTOM_MARGIN - BOX_HEIGHT);
        box.setEffect(new DropShadow(24, 0, 6, Color.rgb(0, 0, 0, 0.8)));

        name.setStyle(SERIF + "-fx-text-fill: #e8d08a; -fx-font-size: 21px; -fx-font-weight: bold;");
        name.setLayoutX(TEXT_INSET);
        name.setLayoutY(16);

        String textStyle = SERIF + "-fx-font-size: 22px;";
        written.setStyle(textStyle + "-fx-fill: #f5ebd0;");
        unwritten.setStyle(textStyle + "-fx-fill: transparent;");

        TextFlow text = new TextFlow(written, unwritten);
        text.setLineSpacing(4);
        text.setPrefWidth(BOX_WIDTH - 2 * TEXT_INSET);
        text.setMaxWidth(BOX_WIDTH - 2 * TEXT_INSET);
        text.setLayoutX(TEXT_INSET);
        text.setLayoutY(52);

        arrow.setFill(Color.TRANSPARENT);
        arrow.setStroke(Color.web("#d2b45a"));
        arrow.setStrokeWidth(2);
        arrow.setLayoutX(BOX_WIDTH - GoldFrame.BEVEL - 58);
        arrow.setLayoutY(BOX_HEIGHT - 48);
        arrow.setMouseTransparent(true);
        bob.setByY(7);
        bob.setCycleCount(Animation.INDEFINITE);
        bob.setAutoReverse(true);

        box.getChildren().addAll(name, text, arrow);
        return box;
    }

    private void placeCharacter(Node character, double x)
    {
        character.setLayoutX(x);
        character.setLayoutY(FEET - CHARACTER);
        character.setMouseTransparent(true);
        root.getChildren().add(character);
    }

    private Node character(String characterName, boolean facingLeft, Color silhouetteColor)
    {
        SpriteSheet idle = SpriteArt.sheet(characterName, AnimatedSprite.IDLE);
        Image still = idle == null ? SpriteArt.find(characterName, facingLeft ? "front" : "back") : null;

        if (idle == null && still == null)
            return Silhouette.of(CHARACTER, CHARACTER, silhouetteColor);

        AnimatedSprite sprite = new AnimatedSprite(CHARACTER, CHARACTER, idle != null && facingLeft);
        sprite.add(AnimatedSprite.IDLE, idle != null ? idle : new SpriteSheet(still, 1), AnimatedSprite.DEFAULT_FPS, true);
        sprite.play(AnimatedSprite.IDLE);
        sprites.add(sprite);

        return sprite;
    }
}
