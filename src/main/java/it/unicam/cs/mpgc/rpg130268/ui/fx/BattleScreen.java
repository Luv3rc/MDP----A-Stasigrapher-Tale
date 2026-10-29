package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenter;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/* *
    The window of a battle: it draws what the BattlePresenter says and tells it what the player does.
    There is no game logic here. This class only puts the pieces together and decides what happens at every frame:

        BattleBackground   the picture of the area, darkened
        EnemyFormation     where the enemies stand
        FighterCard        one sprite with its life bar (one for every fighter)
        PartyPanel         the HUD of a party member (face, life, AP, ink)
        BattleMenu         ATTACK / ABILITIES / ITEMS, and the question to retry
        TargetSelection    A / D / Enter: which target is selected
        BattleAnimator     what the events of the battle look like (cuts, numbers, shakes)
        BattleOverlay      the announcement of an ability and the banner of the end
        RpgButtons         the hexagon buttons (css/battle.css, in the resources)

    How it works:
    - A timer calls frame() every frame: time goes on (the presenter returns the events that have just become
      visible, and they are animated), and everything is drawn again
    - A key is stamped where it happened: before it is handled the battle is moved forward up to that moment,
      not only up to the last frame, so the timing of the player counts exactly
    - The keys that can be used now are written in the bottom-left corner (ESC -- go back, E -- parry...):
      there are no buttons for them
    - Keys: F, E, W = the first, second, third entry of the menu, Esc = back, E = parry, Q = dodge (the menu and the
      attacks never happen together), A / D (or the arrows) and Enter = the target, Y / N = retry the battle or not,
      T = show/hide the timing bar, L = show/hide the log
    - The pictures of the background and of the characters are looked for in the resources, see SpriteArt.
      Without them the game draws plain shapes
* */
public class BattleScreen
{
    private static final long NANOS_PER_MS = 1_000_000;
    private static final long MAX_STEP_MS = 100;       // a long pause (the window was dragged...) is not played at once

    private static final double HUD_MARGIN = 14;
    private static final double BOTTOM_MARGIN = 40;          // under it there is room for the keys that work now
    private static final double PARTY_LEFT = 300;
    private static final double MENU_LEFT = PARTY_LEFT + 150 + 36;   // the menu is on the right of Gunther (150 = his width)
    private static final double ENEMIES_RIGHT = 120;
    private static final double ENEMIES_BOTTOM = 150;        // the feet of the enemies in front: above the head of Gunther

    private final Supplier<BattlePresenter> factory;

    private final BorderPane root = new BorderPane();
    private final Pane slashLayer = new Pane();
    private final StackPane stage = new StackPane(root, slashLayer);
    private final BattleBackground background = new BattleBackground(stage, root);
    private final BattleOverlay overlay = new BattleOverlay();
    private final EnemyFormation enemies = new EnemyFormation();

    private final StackPane field = new StackPane();
    private final HBox partyBox = new HBox(24);
    private final HBox hudBox = new HBox(16);
    private final Label titleLabel = new Label();
    private final Label lockLabel = new Label();
    private final Label hintLabel = new Label();
    private final TimingBar timingBar = new TimingBar();
    private final TextArea logArea = new TextArea();

    private final Map<Fighter, FighterCard> cards = new LinkedHashMap<>();
    private final List<PartyPanel> panels = new ArrayList<>();

    private final BattleMenu menu;
    private final TargetSelection targeting;
    private final BattleAnimator animator;

    private final AnimationTimer timer = new AnimationTimer()
    {
        @Override
        public void handle(long ignored) { frame(System.nanoTime()); }
    };

    private BattlePresenter presenter;
    private long lastNanos;
    private int logShown;
    private boolean timingBarOn = true;

    public BattleScreen(Supplier<BattlePresenter> factory)
    {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");

        // The look of the buttons (src/main/resources/css/battle.css). If it is not there the buttons are plain
        URL stylesheet = BattleScreen.class.getResource("/css/battle.css");
        if (stylesheet != null)
            stage.getStylesheets().add(stylesheet.toExternalForm());

        // The pieces that need "the battle of now" get a way to ask for it: a new presenter is made at every battle
        menu = new BattleMenu(new RpgButtons(stylesheet != null), () -> presenter, this::startNewBattle, Platform::exit);
        targeting = new TargetSelection(cards, () -> presenter);
        animator = new BattleAnimator(cards, new SlashEffect(slashLayer), overlay, () -> presenter.getTimeMs());

        // The cuts go over everything but they must not catch the mouse, and they never draw outside of the window
        slashLayer.setMouseTransparent(true);
        Rectangle slashClip = new Rectangle();
        slashClip.widthProperty().bind(slashLayer.widthProperty());
        slashClip.heightProperty().bind(slashLayer.heightProperty());
        slashLayer.setClip(slashClip);

        // The background is the first thing of the window, under everything. Which picture it is depends on the area
        // of the battle (see startNewBattle)
        stage.getChildren().addAll(0, background.nodes());

        // The content can never ask for more room than the window has (a taller menu, a longer text...): if it did,
        // the StackPane would make the screen bigger than the window and everything would slide
        root.setMinSize(0, 0);

        // Top: the round, whose turn it is, the lockout, and the timing bar (T to hide it)
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

        // Center: the field. Gunther on the left and the enemies on the right, facing each other: the middle of the
        // screen is left free, because that is where the backgrounds have what is worth looking at.
        // The menu is on the right of Gunther, and the HUD in the top-right corner.
        // The enemies are above the head of Gunther: the middle of the screen stays free.
        // Every box has exactly the size of its content: if the StackPane stretched them, the alignment
        // (and the middle of the sprites, where the cuts aim) would be wrong
        partyBox.setAlignment(Pos.BOTTOM_CENTER);
        hudBox.setAlignment(Pos.TOP_RIGHT);
        hudBox.setSpacing(12);
        for (HBox box : List.of(partyBox, hudBox)) box.setFillHeight(false);
        for (Region box : List.of(enemies.getRoot(), partyBox, hudBox, menu.getRoot())) box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(enemies.getRoot(), Pos.BOTTOM_RIGHT);
        StackPane.setMargin(enemies.getRoot(), new Insets(0, ENEMIES_RIGHT, ENEMIES_BOTTOM, 0));
        StackPane.setAlignment(partyBox, Pos.BOTTOM_LEFT);
        StackPane.setMargin(partyBox, new Insets(0, 0, BOTTOM_MARGIN, PARTY_LEFT));
        StackPane.setAlignment(menu.getRoot(), Pos.BOTTOM_LEFT);
        StackPane.setMargin(menu.getRoot(), new Insets(0, 0, BOTTOM_MARGIN, MENU_LEFT));
        StackPane.setAlignment(hudBox, Pos.TOP_RIGHT);
        StackPane.setMargin(hudBox, new Insets(HUD_MARGIN, HUD_MARGIN, 0, 0));

        // The log is out of the way: it is only there if the player wants it (L)
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setFocusTraversable(false);
        logArea.setStyle("-fx-control-inner-background: #14141d; -fx-text-fill: #dddddd; -fx-opacity: 0.92;");
        logArea.setMaxSize(360, 220);
        logArea.setVisible(false);
        StackPane.setAlignment(logArea, Pos.TOP_LEFT);
        StackPane.setMargin(logArea, new Insets(8));

        // The keys that work now, in the bottom-left corner
        hintLabel.setStyle("-fx-text-fill: #ccccdd; -fx-font-size: 14px; -fx-font-weight: bold; "
                + "-fx-effect: dropshadow(gaussian, black, 4, 0.8, 0, 0);");
        hintLabel.setMouseTransparent(true);
        StackPane.setAlignment(hintLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(hintLabel, new Insets(0, 0, 8, 10));

        field.getChildren().addAll(enemies.getRoot(), partyBox, menu.getRoot(), hintLabel, logArea, overlay.bannerNode());

        // The announcement and the HUD are over the WHOLE window (the HUD over the top bar), but under the cuts
        stage.getChildren().add(stage.getChildren().indexOf(slashLayer), overlay.announcementNode());
        stage.getChildren().add(stage.getChildren().indexOf(slashLayer), hudBox);
        root.setCenter(field);
    }

    public Region getRoot() { return stage; }

    // A new fight: the presenter is built again, and the sprites, the HUD, the log and the menu with it
    public void startNewBattle()
    {
        timer.stop();

        presenter = factory.get();
        presenter.setAlwaysAskTarget(true);     // even with a single enemy, the player confirms it
        presenter.start();

        for (FighterCard old : cards.values()) old.dispose();
        cards.clear();
        panels.clear();
        partyBox.getChildren().clear();
        enemies.clear();
        hudBox.getChildren().clear();

        for (BattlePresenter.FighterView view : presenter.getParty())
        {
            partyBox.getChildren().add(makeCard(view).getRoot());

            PartyPanel panel = new PartyPanel(view);
            panels.add(panel);
            hudBox.getChildren().add(panel.getRoot());
        }

        List<FighterCard> enemyCards = new ArrayList<>();
        for (BattlePresenter.FighterView view : presenter.getEnemies()) enemyCards.add(makeCard(view));
        enemies.place(enemyCards);

        background.show(presenter.getArea());
        animator.reset();
        menu.reset();

        logArea.clear();
        logShown = 0;

        lastNanos = System.nanoTime();
        timer.start();
    }

    public void stop()
    {
        timer.stop();
        for (FighterCard card : cards.values()) card.dispose();
    }

    private FighterCard makeCard(BattlePresenter.FighterView view)
    {
        FighterCard card = new FighterCard(view);

        // Clicking a sprite chooses it as the target (the presenter ignores it if nothing is waiting for one)
        card.getRoot().setOnMouseClicked(event -> presenter.chooseTarget(view.getFighter()));

        cards.put(view.getFighter(), card);
        return card;
    }

    // ---------- Time ----------

    private void frame(long nowNanos)
    {
        long elapsedMs = (nowNanos - lastNanos) / NANOS_PER_MS;

        if (elapsedMs > 0)
        {
            // The leftover nanoseconds are kept for the next frame: no time is lost
            lastNanos += elapsedMs * NANOS_PER_MS;

            for (BattleEvent event : presenter.advance(Math.min(elapsedMs, MAX_STEP_MS)))
                animator.animate(event);
        }

        render();
    }

    // ---------- Keys ----------

    public void onKeyPressed(KeyEvent event)
    {
        if (presenter == null) return;

        KeyCode code = event.getCode();

        // What a key does depends on what the player is doing: choosing in the menu (F, E, W...), choosing the
        // target (A, D, Enter), reacting to an attack (E, Q). They never happen together, so E can be both
        if (code == KeyCode.T)
            timingBarOn = !timingBarOn;
        else if (code == KeyCode.L)
            logArea.setVisible(!logArea.isVisible());
        else if (presenter.isOver())
            retryKey(code);
        else if (presenter.isTargeting())
            targetKey(code);
        else if (presenter.isChoosing())
            menu.onKey(code);
        else if (code == KeyCode.E)
            react(ReactionType.PARRY);
        else if (code == KeyCode.Q)
            react(ReactionType.DODGE);
    }

    private void retryKey(KeyCode code)
    {
        if (code == KeyCode.Y)
            startNewBattle();
        else if (code == KeyCode.N)
            Platform.exit();
    }

    private void targetKey(KeyCode code)
    {
        if (code == KeyCode.A || code == KeyCode.LEFT)
            targeting.move(-1);
        else if (code == KeyCode.D || code == KeyCode.RIGHT)
            targeting.move(1);
        else if (code == KeyCode.ENTER)
            targeting.confirm();
        else if (code == KeyCode.ESCAPE)
            presenter.cancel();
    }

    // The battle is moved forward up to THIS moment first, so the key is stamped where it happened
    private void react(ReactionType type)
    {
        frame(System.nanoTime());
        presenter.press(type);
    }

    // ---------- Drawing ----------

    private void render()
    {
        String actor = presenter.getCurrentActor().map(Fighter::getName).orElse("-");
        titleLabel.setText("Round " + presenter.getRound() + "   |   " + actor + (presenter.isChoosing() ? "   (your turn)" : ""));
        lockLabel.setText(presenter.isLocked() ? "KEYS LOCKED  " + presenter.getLockedRemainingMs() + " ms" : "");

        if (!presenter.isTargeting()) targeting.reset();

        List<Fighter> candidates = presenter.isTargeting() ? targeting.candidates() : List.of();
        Fighter selected = targeting.selected(candidates);

        for (Map.Entry<Fighter, FighterCard> entry : cards.entrySet())
        {
            Fighter fighter = entry.getKey();
            boolean active = presenter.getCurrentActor().isPresent() && presenter.getCurrentActor().get() == fighter;

            entry.getValue().update(active, candidates.contains(fighter), fighter == selected);
        }

        hintLabel.setText(hint());

        for (PartyPanel panel : panels) panel.update();

        if (timingBarOn)
            presenter.getCue().ifPresentOrElse(timingBar::show, timingBar::hide);
        else
            timingBar.hide();

        animator.update();

        List<String> log = presenter.getLog();
        for (; logShown < log.size(); logShown++)
            logArea.appendText(log.get(logShown) + "\n");

        menu.update();
    }

    // The keys that work now, written in the bottom-left corner (there are no buttons for them)
    private String hint()
    {
        if (presenter.isOver())
            return "";

        if (presenter.isTargeting())
            return "A / D -- change target        ENTER -- confirm        ESC -- go back";

        if (presenter.isChoosing())
            return menu.isOnAbilitiesPage() ? "ESC -- go back" : "";

        return "E -- parry        Q -- dodge";
    }
}