package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.defeat.DefeatPresenter;
import it.unicam.cs.mpgc.rpg130268.ui.defeat.DefeatPresenter.Action;
import it.unicam.cs.mpgc.rpg130268.ui.defeat.DefeatPresenter.View;
import it.unicam.cs.mpgc.rpg130268.ui.question.Choice;
import it.unicam.cs.mpgc.rpg130268.ui.question.QuestionView;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.util.Objects;
import java.util.Optional;

/* *
    It draws what the DefeatPresenter says and tells it the time and what the player does
* */
final class DefeatScreen implements GameScreen
{
    private static final long NANOS_PER_MS = 1_000_000;
    private static final long MAX_STEP_MS = 100;

    private final StackPane root = new StackPane();
    private final DefeatPresenter presenter = new DefeatPresenter();
    private final Region veil = new Region();
    private final Label phrase = new Label();
    private final QuestionPopup popup;
    private final Runnable onRetry;
    private final Runnable onGoBack;

    private final AnimationTimer timer = new AnimationTimer()
    {
        @Override
        public void handle(long ignored) { frame(System.nanoTime()); }
    };

    private long lastNanos;
    private QuestionView shown;

    DefeatScreen(String area, Runnable onRetry, Runnable onGoBack)
    {
        Objects.requireNonNull(area, "area must not be null");
        this.onRetry = Objects.requireNonNull(onRetry, "onRetry must not be null");
        this.onGoBack = Objects.requireNonNull(onGoBack, "onGoBack must not be null");

        BattleBackground background = new BattleBackground(root, root);
        root.getChildren().addAll(background.nodes());
        background.show(area);

        veil.setStyle("-fx-background-color: black;");
        veil.setOpacity(0);
        veil.setMouseTransparent(true);

        phrase.setText(DefeatPresenter.PHRASE);
        phrase.setStyle(GoldFrame.SERIF + "-fx-text-fill: #b3121c; -fx-font-size: 76px; -fx-font-weight: bold;");
        phrase.setWrapText(true);
        phrase.setTextAlignment(TextAlignment.CENTER);
        phrase.setAlignment(Pos.CENTER);
        phrase.setMaxWidth(1000);
        phrase.setEffect(new DropShadow(30, Color.web("#5a0006")));
        phrase.setOpacity(0);
        phrase.setMouseTransparent(true);

        popup = new QuestionPopup(new RpgButtons(GameStyle.apply(root)),
                () -> answer(presenter.choose(Choice.CONFIRM)), () -> answer(presenter.choose(Choice.CANCEL)));

        root.getChildren().addAll(veil, phrase, popup.getRoot());
    }

    @Override
    public Region getRoot() { return root; }

    @Override
    public void onShow()
    {
        lastNanos = System.nanoTime();
        timer.start();
    }

    @Override
    public void stop() { timer.stop(); }

    @Override
    public void onKeyPressed(KeyEvent event)
    {
        switch (event.getCode())
        {
            case A, LEFT, D, RIGHT ->
            {
                event.consume();
                presenter.move();
                draw();
            }
            case ENTER, SPACE ->
            {
                event.consume();
                answer(presenter.activate());
            }
            case Y ->
            {
                event.consume();
                answer(presenter.choose(Choice.CONFIRM));
            }
            case N ->
            {
                event.consume();
                answer(presenter.choose(Choice.CANCEL));
            }
            default -> { }
        }
    }


    // ---------- Inside ----------

    private void frame(long nowNanos)
    {
        long ms = Math.min(MAX_STEP_MS, (nowNanos - lastNanos) / NANOS_PER_MS);
        lastNanos += ms * NANOS_PER_MS;

        presenter.tick(ms);
        draw();
    }

    // The black veil, the phrase with its opacity
    private void draw()
    {
        View view = presenter.view();
        veil.setOpacity(view.darkness());
        phrase.setOpacity(view.phraseOpacity());

        Optional<QuestionView> question = view.question();

        if (question.isEmpty())
        {
            popup.hide();
            shown = null;
        }
        else if (!question.get().equals(shown))
        {
            popup.show(question.get());
            shown = question.get();
        }
    }

    private void answer(Action action)
    {
        switch (action)
        {
            case RETRY ->
            {
                timer.stop();
                onRetry.run();
            }
            case GO_BACK ->
            {
                timer.stop();
                onGoBack.run();
            }
            case NONE -> { }
        }
    }
}
