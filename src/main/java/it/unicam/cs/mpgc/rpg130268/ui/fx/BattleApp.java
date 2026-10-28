package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.DemoBattles;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/* *
    The JavaFX application: a window with a demo battle. Started by Main

    The game is always 16:9:
    - The battle is drawn on a fixed 1280 x 720 "canvas", which is scaled to fit the window (everything gets bigger
      with the window, the buttons too). If the window is not 16:9 (full screen on another screen, for example)
      there are black bars: the picture is never stretched
    - The window itself keeps the 16:9 shape when it is resized
* */
public class BattleApp extends Application
{
    private static final double DESIGN_WIDTH = 1280;
    private static final double DESIGN_HEIGHT = 720;
    private static final double ASPECT = DESIGN_WIDTH / DESIGN_HEIGHT;
    private static final double START_WIDTH = 1152;           // 16:9, small enough for a laptop screen
    private static final double MIN_WIDTH = 640;

    private BattleScreen screen;
    private boolean adjusting = false;                        // the window is being corrected: do not correct it again

    @Override
    public void start(Stage stage)
    {
        screen = new BattleScreen(DemoBattles::create);

        Region canvas = screen.getRoot();
        canvas.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        canvas.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        canvas.setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);

        // Whatever sticks out of the canvas (a shadow, a long text) is cut: it must not change the size the Group
        // has, because the window keeps the Group in the middle and the whole picture would move
        canvas.setClip(new Rectangle(DESIGN_WIDTH, DESIGN_HEIGHT));

        // A Group takes the size of what it holds AFTER the scale, and the StackPane keeps it in the middle
        StackPane window = new StackPane(new Group(canvas));
        window.setStyle("-fx-background-color: black;");

        Scene scene = new Scene(window, START_WIDTH, START_WIDTH / ASPECT);

        Runnable fit = () ->
        {
            double scale = Math.min(scene.getWidth() / DESIGN_WIDTH, scene.getHeight() / DESIGN_HEIGHT);
            canvas.setScaleX(scale);
            canvas.setScaleY(scale);
        };
        scene.widthProperty().addListener((observable, old, width) -> fit.run());
        scene.heightProperty().addListener((observable, old, height) -> fit.run());
        fit.run();

        // A FILTER: the keys reach the screen before any control (a focused button or the log) can take them
        scene.addEventFilter(KeyEvent.KEY_PRESSED, screen::onKeyPressed);

        stage.setTitle("A Stasigrapher Tale");
        stage.setScene(scene);
        stage.show();

        keepRatio(stage, scene);

        screen.startNewBattle();
    }

    // The window cannot become any other shape than 16:9. What the title bar and the borders take is measured
    // once the window is shown
    private void keepRatio(Stage stage, Scene scene)
    {
        double extraWidth = stage.getWidth() - scene.getWidth();
        double extraHeight = stage.getHeight() - scene.getHeight();

        stage.setMinWidth(MIN_WIDTH + extraWidth);
        stage.setMinHeight(MIN_WIDTH / ASPECT + extraHeight);

        scene.widthProperty().addListener((observable, old, width) ->
        {
            if (canAdjust(stage))
            {
                adjusting = true;
                stage.setHeight(width.doubleValue() / ASPECT + extraHeight);
                adjusting = false;
            }
        });

        scene.heightProperty().addListener((observable, old, height) ->
        {
            if (canAdjust(stage))
            {
                adjusting = true;
                stage.setWidth(height.doubleValue() * ASPECT + extraWidth);
                adjusting = false;
            }
        });
    }

    // Maximized or in full screen the window cannot be changed: there the black bars do the job
    private boolean canAdjust(Stage stage)
    {
        return !adjusting && !stage.isMaximized() && !stage.isFullScreen();
    }

    @Override
    public void stop()
    {
        if (screen != null) screen.stop();
    }
}