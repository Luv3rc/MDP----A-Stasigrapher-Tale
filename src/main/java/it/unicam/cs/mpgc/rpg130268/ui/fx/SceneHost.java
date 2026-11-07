package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

/* *
    The window of the game. It shows one GameScreen at a time and changes it when asked (show), with a short fade

    The game is always 16:9.
    - Every screen is drawn on a fixed 1280 x 720 canvas, which is scaled to fit the window, so everything gets
      bigger with the window, buttons included. If the window is not 16:9 (full screen on another monitor, for
      example) there are black bars, and the picture is not stretched
    - The window itself keeps the 16:9 shape when it is resized.
    - The keys go to the screen on view, before any control can take them
* */
final class SceneHost
{
    static final double DESIGN_WIDTH = 1280;
    static final double DESIGN_HEIGHT = 720;

    private static final double ASPECT = DESIGN_WIDTH / DESIGN_HEIGHT;
    private static final double START_WIDTH = 1152;           // 16:9 and small enough for a laptop screen
    private static final double MIN_WIDTH = 640;
    private static final double FADE_MS = 250;

    private final Stage stage;
    private final Group holder = new Group();
    private final Scene scene;

    private GameScreen current;
    private boolean adjusting = false;                        // the window is being corrected, so it must not be corrected again

    SceneHost(Stage stage, String title)
    {
        this.stage = stage;

        StackPane window = new StackPane(holder);
        window.setStyle("-fx-background-color: black;");

        scene = new Scene(window, START_WIDTH, START_WIDTH / ASPECT);
        scene.widthProperty().addListener((observable, old, width) -> fit());
        scene.heightProperty().addListener((observable, old, height) -> fit());

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            if (current != null)
                current.onKeyPressed(event);
        });

        stage.setTitle(title);
        stage.setScene(scene);
    }

    void start()
    {
        stage.show();
        keepRatio();
    }

    void show(GameScreen screen)
    {
        GameScreen old = current;
        current = screen;

        Region root = screen.getRoot();
        root.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        root.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        root.setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);

        // Whatever sticks out of the canvas (a shadow, a long text) is cut. It must not change the size of the
        // Group, because the window keeps the Group in the middle and the whole picture would move
        root.setClip(new Rectangle(DESIGN_WIDTH, DESIGN_HEIGHT));

        holder.getChildren().setAll(root);
        fit();

        if (old != null)
            old.stop();

        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(FADE_MS), root);
        fade.setToValue(1);
        fade.play();

        screen.onShow();
    }

    void stop()
    {
        if (current != null)
            current.stop();
    }

    private void fit()
    {
        if (current == null)
            return;

        double scale = Math.min(scene.getWidth() / DESIGN_WIDTH, scene.getHeight() / DESIGN_HEIGHT);
        current.getRoot().setScaleX(scale);
        current.getRoot().setScaleY(scale);
    }


    private void keepRatio()
    {
        double extraWidth = stage.getWidth() - scene.getWidth();
        double extraHeight = stage.getHeight() - scene.getHeight();

        stage.setMinWidth(MIN_WIDTH + extraWidth);
        stage.setMinHeight(MIN_WIDTH / ASPECT + extraHeight);

        scene.widthProperty().addListener((observable, old, width) ->
        {
            if (canAdjust())
            {
                adjusting = true;
                stage.setHeight(width.doubleValue() / ASPECT + extraHeight);
                adjusting = false;
            }
        });

        scene.heightProperty().addListener((observable, old, height) ->
        {
            if (canAdjust())
            {
                adjusting = true;
                stage.setWidth(height.doubleValue() * ASPECT + extraWidth);
                adjusting = false;
            }
        });
    }

    private boolean canAdjust()
    {
        return !adjusting && !stage.isMaximized() && !stage.isFullScreen();
    }
}
