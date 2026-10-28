package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOutcome;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.BasicAttack;
import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenter;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/* *
    The window of a battle: it draws what the BattlePresenter says and tells it what the player does.
    There is no game logic here.

    What is on the screen:
    - the enemies in front (upper part), Gunther and the party from behind (bottom-left), facing them
    - the menu next to Gunther: ATTACK / ABILITIES / ITEMS. ABILITIES opens a second menu (two columns of three)
    - the HUD of every party member in the top-right corner (face, life, AP, ink: see PartyPanel)
    - the cuts of the attacks (SlashEffect), on a transparent layer over everything

    How it works:
    - A timer calls frame() every frame: time goes on (the presenter returns the events that have just become
      visible, and they are animated), and everything is drawn again
    - A key is stamped where it happened: before it is handled the battle is moved forward up to that moment,
      not only up to the last frame, so the timing of the player counts exactly
    - The menu is built again only when it changes, so a button is never replaced while the mouse is on it
    - The keys that can be used now are written in the bottom-left corner (ESC -- go back, E -- parry...):
      there are no buttons for them
    - The target is chosen on the battlefield, not in a menu: A / D (or the arrows) move the selection from one
      sprite to the next, Enter confirms (a sprite can also be clicked)
    - Keys: F, E, W = the first, second, third entry of the menu, Esc = back, E = parry, Q = dodge (the menu and the
      attacks never happen together), Y / N = retry the battle or not, T = show/hide the timing bar,
      L = show/hide the log
    - The buttons are hexagons with a thin silver border (css/battle.css, in the resources). The pictures of the background and of the
      characters are looked for in the resources too, see SpriteArt. Without them the game draws plain shapes
    - The background depends on the area of the battle (BattlePresenter.getArea): every new battle shows its own
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
    private static final double ENEMY_SPACING = 190;         // between two enemies of the same row
    private static final double ENEMY_DEPTH = 60;            // how much lower the enemies of the front row are
    private static final double ENEMY_CARD_WIDTH = 130;
    private static final double ENEMY_CARD_HEIGHT = 252;     // sprite, name and life bar
    private static final String DAMAGE_COLOR = "#e74c3c";    // the colors of the numbers over the sprites
    private static final String CRIT_COLOR = "#e056fd";      // a critical hit: not like any other number
    private static final long ANNOUNCEMENT_FADE_IN_MS = 250;
    private static final long ANNOUNCEMENT_FADE_OUT_MS = 350;
    private static final double BACKGROUND_DIM = 0.35;       // how much the background is darkened (0 = not at all)

    private static final double MENU_BUTTON_WIDTH = 210;
    private static final double MENU_BUTTON_HEIGHT = 52;
    private static final double ABILITY_WIDTH = 272;              // with more than three abilities: ABILITY_WIDTH_NARROW
    private static final double ABILITY_WIDTH_NARROW = 262;       // (two columns must not reach Gunther)
    private static final double BORDER = 1;                       // the thin border of a button (see battle.css)
    private static final double ABILITY_HEIGHT = 75;          // three of them (with the gaps) are not taller than Gunther
    private static final double ABILITY_GAP = 6;

    // The keys of the menu, in order: the first entry is F, the second E, the third W...
    private static final List<KeyCode> MENU_KEYS = List.of(KeyCode.F, KeyCode.E, KeyCode.W, KeyCode.R, KeyCode.G, KeyCode.V);

    private static final String PAGE_MAIN = "main";
    private static final String PAGE_ABILITIES = "abilities";

    private static final String BUTTON_STYLE = "-fx-background-color: #14141d; -fx-text-fill: white; -fx-font-size: 20px; "
            + "-fx-border-color: #e8e8f0; -fx-border-width: 2; -fx-background-radius: 0; -fx-border-radius: 0;";

    private final Supplier<BattlePresenter> factory;

    private final BorderPane root = new BorderPane();
    private final Pane slashLayer = new Pane();
    private final StackPane stage = new StackPane(root, slashLayer);
    private final ImageView backgroundView = new ImageView();
    private final Rectangle backgroundDim = new Rectangle();  // darkens the background so the rest is easy to read
    private final SlashEffect slashes = new SlashEffect(slashLayer);

    private final StackPane field = new StackPane();
    private final StackPane enemyField = new StackPane();     // the enemies, in formation (see placeEnemies)
    private final HBox partyBox = new HBox(24);
    private final HBox hudBox = new HBox(16);
    private final VBox menuBox = new VBox(10);
    private final Label titleLabel = new Label();
    private final Label lockLabel = new Label();
    private final Label bannerLabel = new Label();
    private final Label hintLabel = new Label();
    private final Label announcementLabel = new Label();     // the text of an ability that announces itself
    private final TimingBar timingBar = new TimingBar();
    private final TextArea logArea = new TextArea();

    private final Map<Fighter, FighterCard> cards = new LinkedHashMap<>();
    private final List<PartyPanel> panels = new ArrayList<>();

    // The moment (battle clock) when a fighter has to start its attack animation so that its blow lands with the hit
    private record AttackCue(long atMs, FighterCard card) { }
    private final List<AttackCue> attackCues = new ArrayList<>();
    private final AnimationTimer timer = new AnimationTimer()
    {
        @Override
        public void handle(long ignored) { frame(System.nanoTime()); }
    };

    private BattlePresenter presenter;
    private long lastNanos;
    private int logShown;
    private String commandMode = "";
    private String page = PAGE_MAIN;
    private boolean timingBarOn = true;
    private SequentialTransition announcement = null;        // the one that is on the screen now, if any
    private boolean buttonStyleLoaded = false;   // battle.css found: the buttons are hexagons with a silver border
    private int targetIndex = 0;              // which of the candidates (from the left) is selected

    public BattleScreen(Supplier<BattlePresenter> factory)
    {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");

        // The look of the buttons (src/main/resources/css/battle.css). If it is not there the buttons are plain
        URL stylesheet = BattleScreen.class.getResource("/css/battle.css");
        if (stylesheet != null)
        {
            stage.getStylesheets().add(stylesheet.toExternalForm());
            buttonStyleLoaded = true;
        }

        // The cuts go over everything but they must not catch the mouse, and they never draw outside of the window
        slashLayer.setMouseTransparent(true);
        Rectangle slashClip = new Rectangle();
        slashClip.widthProperty().bind(slashLayer.widthProperty());
        slashClip.heightProperty().bind(slashLayer.heightProperty());
        slashLayer.setClip(slashClip);

        // The background fills the window, a bit darkened. Which picture it is depends on the area of the battle
        // (see startNewBattle).
        // The background is outside of the layout (not managed): nothing that changes in the menu can move it or
        // change its size, it is always the whole window, from its corner
        backgroundView.setPreserveRatio(false);
        backgroundView.setManaged(false);
        backgroundView.setMouseTransparent(true);
        backgroundView.fitWidthProperty().bind(stage.widthProperty());
        backgroundView.fitHeightProperty().bind(stage.heightProperty());

        backgroundDim.setManaged(false);
        backgroundDim.setMouseTransparent(true);
        backgroundDim.setFill(Color.rgb(0, 0, 0, BACKGROUND_DIM));
        backgroundDim.widthProperty().bind(stage.widthProperty());
        backgroundDim.heightProperty().bind(stage.heightProperty());

        // The content can never ask for more room than the window has (a taller menu, a longer text...): if it did,
        // the StackPane would make the screen bigger than the window and everything would slide
        root.setMinSize(0, 0);

        stage.getChildren().addAll(0, List.of(backgroundView, backgroundDim));

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
        enemyField.setAlignment(Pos.BOTTOM_CENTER);
        partyBox.setAlignment(Pos.BOTTOM_CENTER);
        hudBox.setAlignment(Pos.TOP_RIGHT);
        hudBox.setSpacing(12);
        menuBox.setAlignment(Pos.BOTTOM_LEFT);
        for (HBox box : List.of(partyBox, hudBox)) box.setFillHeight(false);
        for (Region box : List.of(enemyField, partyBox, hudBox, menuBox)) box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane.setAlignment(enemyField, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(enemyField, new Insets(0, ENEMIES_RIGHT, ENEMIES_BOTTOM, 0));
        StackPane.setAlignment(partyBox, Pos.BOTTOM_LEFT);
        StackPane.setMargin(partyBox, new Insets(0, 0, BOTTOM_MARGIN, PARTY_LEFT));
        StackPane.setAlignment(menuBox, Pos.BOTTOM_LEFT);
        StackPane.setMargin(menuBox, new Insets(0, 0, BOTTOM_MARGIN, MENU_LEFT));
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

        // The announcement: the text of an ability that announces itself ("Mattias27 heals!"), in the top-center of the
        // window. The engine makes the ability begin only after it is gone
        announcementLabel.setStyle("-fx-text-fill: #f5ebd0; -fx-font-size: 16px; -fx-font-weight: bold; "
                + "-fx-background-color: rgba(10, 10, 14, 0.78); -fx-border-color: #d2b45a; -fx-border-width: 1.5; "
                + "-fx-padding: 4 20 4 20; -fx-effect: dropshadow(gaussian, black, 6, 0.4, 0, 1);");
        announcementLabel.setVisible(false);
        announcementLabel.setMouseTransparent(true);
        StackPane.setAlignment(announcementLabel, Pos.TOP_CENTER);
        StackPane.setMargin(announcementLabel, new Insets(12, 0, 0, 0));
        stage.getChildren().add(stage.getChildren().indexOf(slashLayer), announcementLabel);

        bannerLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 44px; -fx-font-weight: bold; "
                + "-fx-effect: dropshadow(gaussian, black, 8, 0.8, 0, 0);");
        bannerLabel.setVisible(false);
        bannerLabel.setMouseTransparent(true);

        field.getChildren().addAll(enemyField, partyBox, menuBox, hintLabel, logArea, bannerLabel);

        // The HUD is in the top-right corner of the WHOLE window, over the top bar (under the cuts)
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
        enemyField.getChildren().clear();
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
        placeEnemies(enemyCards);

        showBackground(presenter.getArea());
        attackCues.clear();

        logArea.clear();
        logShown = 0;
        slashes.clear();
        if (announcement != null) announcement.stop();
        announcementLabel.setVisible(false);
        bannerLabel.setVisible(false);
        commandMode = "";
        page = PAGE_MAIN;

        lastNanos = System.nanoTime();
        timer.start();
    }

    public void stop()
    {
        timer.stop();
        for (FighterCard card : cards.values()) card.dispose();
    }

    // The background of the area: background_<area>.png, otherwise the general background.png, otherwise a dark color
    private void showBackground(String area)
    {
        Image picture = SpriteArt.background(area);

        backgroundView.setImage(picture);
        backgroundDim.setVisible(picture != null);
        root.setStyle(picture == null ? "-fx-background-color: #1b1b27;" : "-fx-background-color: transparent;");
    }

    private FighterCard makeCard(BattlePresenter.FighterView view)
    {
        FighterCard card = new FighterCard(view);

        // Clicking a sprite chooses it as the target (the presenter ignores it if nothing is waiting for one)
        card.getRoot().setOnMouseClicked(event -> presenter.chooseTarget(view.getFighter()));

        cards.put(view.getFighter(), card);
        return card;
    }

    /* *
        The enemies in formation, like on a stage: a back row and, in front of it and lower, a second row that stands
        in the gaps of the first. The first enemies fill the back row, the others the front row:
            1 enemy:   o                2 enemies:  o   o               3 enemies:  o   o
                                                                                      o
        The enemies of the front row are in the middle of two of the back row, and lower (ENEMY_DEPTH).
        The cards are not moved by the layout but by a holder, so their own animations (the shake of a hit)
        keep working on their own
    * */
    private void placeEnemies(List<FighterCard> enemyCards)
    {
        int count = enemyCards.size();
        int back = (count + 2) / 2;                     // half of them, rounded up (and the front row fits in the gaps)
        int front = count - back;

        enemyField.setPrefSize((back - 1) * ENEMY_SPACING + ENEMY_CARD_WIDTH,
                ENEMY_CARD_HEIGHT + (front > 0 ? ENEMY_DEPTH : 0));

        for (int i = 0; i < count; i++)
        {
            boolean inBackRow = i < back;
            int index = inBackRow ? i : i - back;
            int rowSize = inBackRow ? back : front;

            StackPane holder = new StackPane(enemyCards.get(i).getRoot());
            holder.setAlignment(Pos.BOTTOM_CENTER);
            holder.setTranslateX((index - (rowSize - 1) / 2.0) * ENEMY_SPACING);
            holder.setTranslateY(inBackRow && front > 0 ? -ENEMY_DEPTH : 0);

            enemyField.getChildren().add(holder);
        }
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
                animate(event);
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
            menuKey(code);
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
            moveTarget(-1);
        else if (code == KeyCode.D || code == KeyCode.RIGHT)
            moveTarget(1);
        else if (code == KeyCode.ENTER)
            confirm();
        else if (code == KeyCode.ESCAPE)
            presenter.cancel();
    }

    private void menuKey(KeyCode code)
    {
        if (code == KeyCode.ESCAPE)
            page = PAGE_MAIN;
        else if (MENU_KEYS.contains(code))
            pick(MENU_KEYS.indexOf(code) + 1);
    }

    // The battle is moved forward up to THIS moment first, so the key is stamped where it happened
    private void react(ReactionType type)
    {
        frame(System.nanoTime());
        presenter.press(type);
    }

    // The selection goes to the next sprite on the left (-1) or on the right (+1), and round again
    private void moveTarget(int step)
    {
        int count = orderedCandidates().size();
        if (count > 0) targetIndex = Math.floorMod(targetIndex + step, count);
    }

    // Enter: the selected sprite is the target
    private void confirm()
    {
        List<Fighter> candidates = orderedCandidates();
        if (!candidates.isEmpty())
            presenter.chooseTarget(candidates.get(Math.floorMod(targetIndex, candidates.size())));
    }

    // The fighters that can be chosen, from the left of the screen to the right (so A and D go where they say)
    private List<Fighter> orderedCandidates()
    {
        return presenter.getTargetCandidates().stream()
                .sorted(Comparator.comparingDouble(this::screenX))
                .toList();
    }

    private double screenX(Fighter fighter)
    {
        FighterCard card = cards.get(fighter);
        return card == null ? 0 : card.getRoot().localToScene(card.getRoot().getBoundsInLocal()).getMinX();
    }

    // The n-th entry of the menu (1 = first, written with the key MENU_KEYS.get(n - 1))
    private void pick(int number)
    {
        if (page.equals(PAGE_MAIN))
        {
            Ability basic = basicAttack();

            if (number == 1 && basic != null && basic.canPerform())
                presenter.chooseAbility(basic);
            else if (number == 2 && !otherAbilities().isEmpty())
                page = PAGE_ABILITIES;
        }
        else
        {
            List<Ability> abilities = otherAbilities();
            if (number <= abilities.size() && abilities.get(number - 1).canPerform())
                presenter.chooseAbility(abilities.get(number - 1));
        }
    }

    // The plain strike is the ATTACK of the menu; everything else is under ABILITIES
    private Ability basicAttack()
    {
        return presenter.getAbilities().stream().filter(a -> a instanceof BasicAttack).findFirst().orElse(null);
    }

    private List<Ability> otherAbilities()
    {
        return presenter.getAbilities().stream().filter(a -> !(a instanceof BasicAttack)).toList();
    }

    // ---------- Animations ----------

    private void animate(BattleEvent event)
    {
        switch (event)
        {
            case BattleEvent.HitResolved e ->
            {
                slashEffect(e);
                hitEffect(e);
            }
            case BattleEvent.Countered e ->
            {
                FighterCard card = cards.get(e.attacker());
                if (card != null)
                {
                    slashes.cut(e.attacker(), card.getTarget(), e.critical(), isParty(e.defender()), presenter.getTimeMs());
                    card.floatText("COUNTER -" + e.damage() + (e.critical() ? "!" : ""), e.critical() ? CRIT_COLOR : "#e67e22", e.critical() ? 30 : 22);
                    card.shake();
                }
            }
            case BattleEvent.Healed e ->
            {
                FighterCard card = cards.get(e.target());
                if (card != null) card.floatText("+" + e.amount(), "#2ecc71", 22);
            }
            case BattleEvent.BattleEnded e -> showBanner(e);
            case BattleEvent.TurnStarted e -> { }
            case BattleEvent.Announced e -> showAnnouncement(e);
            case BattleEvent.AttackStarted e -> scheduleSlashes(e);
            case BattleEvent.Swing e -> scheduleSwing(e);
            case BattleEvent.Defeated e ->
            {
                FighterCard card = cards.get(e.fighter());
                if (card != null) card.defeat();
            }
        }
    }

    // An enemy attack has begun: every hit has its blade, which will be on its target exactly when the hit lands.
    // The hit i starts when the hit i-1 lands (the same rule the presenter uses for the timing bar)
    private void scheduleSlashes(BattleEvent.AttackStarted e)
    {
        boolean fromParty = isParty(e.attacker());
        long start = e.atMs();

        for (Hit hit : e.hits())
        {
            long impact = start + hit.windUpMs();

            for (Fighter target : e.targets())
            {
                FighterCard card = cards.get(target);
                if (card != null)
                    slashes.schedule(e.attacker(), target, card.getTarget(), start, impact, hit.strikeWeight(), fromParty);
            }

            queueAttack(e.attacker(), impact);
            start = impact;
        }
    }

    // A strike of the party (it cannot be reacted to): the blade comes toward the target during the wind-up, and the
    // attacker strikes so that its blow lands with it. Then the HitResolved says what happened
    private void scheduleSwing(BattleEvent.Swing e)
    {
        boolean fromParty = isParty(e.attacker());
        long impact = e.atMs() + e.hit().windUpMs();

        for (Fighter target : e.targets())
        {
            FighterCard card = cards.get(target);
            if (card != null)
                slashes.schedule(e.attacker(), target, card.getTarget(), e.atMs(), impact, e.hit().strikeWeight(), fromParty);
        }

        queueAttack(e.attacker(), impact);
    }

    // The attacker's strike starts a bit before the impact: as long as its animation needs to reach the blow
    private void queueAttack(Fighter attacker, long impactMs)
    {
        FighterCard card = cards.get(attacker);
        if (card != null)
            attackCues.add(new AttackCue(impactMs - card.attackLeadMs(), card));
    }

    // The hit has been judged: its blade learns how it ended (or a fast cut is drawn, for the strikes of the player)
    private void slashEffect(BattleEvent.HitResolved e)
    {
        FighterCard card = cards.get(e.target());
        if (card == null) return;

        ReactionOutcome reaction = e.reaction();
        SlashEffect.Outcome outcome = reaction.isSuccessfulParry() ? SlashEffect.Outcome.PARRIED
                : reaction.avoidsDamage() ? SlashEffect.Outcome.DODGED
                  : SlashEffect.Outcome.HIT;

        slashes.resolve(e.attacker(), e.target(), card.getTarget(), outcome, e.critical(), isParty(e.attacker()), presenter.getTimeMs());
    }

    private boolean isParty(Fighter fighter)
    {
        FighterCard card = cards.get(fighter);
        return card != null && card.getView().isPartyMember();
    }

    private void hitEffect(BattleEvent.HitResolved e)
    {
        FighterCard card = cards.get(e.target());
        if (card == null) return;

        if (e.reaction().isSuccessfulParry())
        {
            card.floatText("PARRY!", "#f1c40f", 24);
            card.play("parry");
        }
        else if (e.reaction().avoidsDamage())
        {
            card.floatText("DODGE!", "#3498db", 24);
            card.sidestep();
            card.play("dodge");
        }
        else
        {
            card.floatText("-" + e.damage() + (e.critical() ? "!" : ""), e.critical() ? CRIT_COLOR : DAMAGE_COLOR, e.critical() ? 34 : 22);
            card.shake();
            card.play("hit");
        }
    }

    // The text fades in, stays, and fades out so that it is gone exactly when the engine starts the ability
    private void showAnnouncement(BattleEvent.Announced e)
    {
        if (announcement != null) announcement.stop();

        announcementLabel.setText(e.text());
        announcementLabel.setOpacity(0);
        announcementLabel.setVisible(true);

        FadeTransition in = new FadeTransition(Duration.millis(ANNOUNCEMENT_FADE_IN_MS), announcementLabel);
        in.setFromValue(0);
        in.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.millis(
                Math.max(0, e.durationMs() - ANNOUNCEMENT_FADE_IN_MS - ANNOUNCEMENT_FADE_OUT_MS)));

        FadeTransition out = new FadeTransition(Duration.millis(ANNOUNCEMENT_FADE_OUT_MS), announcementLabel);
        out.setFromValue(1);
        out.setToValue(0);

        announcement = new SequentialTransition(in, stay, out);
        announcement.setOnFinished(event -> announcementLabel.setVisible(false));
        announcement.play();
    }

    private void showBanner(BattleEvent.BattleEnded e)
    {
        String text = switch (e.outcome())
        {
            case VICTORY -> "VICTORY!   +" + e.xp() + " XP";
            case DEFEAT -> "DEFEAT";
            case ONGOING -> "";
        };

        bannerLabel.setText(text);
        bannerLabel.setVisible(true);
    }

    // ---------- Drawing ----------

    private void render()
    {
        String actor = presenter.getCurrentActor().map(Fighter::getName).orElse("-");
        titleLabel.setText("Round " + presenter.getRound() + "   |   " + actor + (presenter.isChoosing() ? "   (your turn)" : ""));
        lockLabel.setText(presenter.isLocked() ? "KEYS LOCKED  " + presenter.getLockedRemainingMs() + " ms" : "");

        // Every turn starts from the main menu
        if (!presenter.isChoosing()) page = PAGE_MAIN;

        if (!presenter.isTargeting()) targetIndex = 0;

        List<Fighter> candidates = presenter.isTargeting() ? orderedCandidates() : List.of();
        Fighter selected = candidates.isEmpty() ? null : candidates.get(Math.floorMod(targetIndex, candidates.size()));

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

        slashes.update(presenter.getTimeMs());

        long now = presenter.getTimeMs();
        attackCues.removeIf(cue ->
        {
            if (cue.atMs() > now) return false;

            cue.card().attack();
            return true;
        });

        List<String> log = presenter.getLog();
        for (; logShown < log.size(); logShown++)
            logArea.appendText(log.get(logShown) + "\n");

        renderMenu();
    }

    // The menu is built again only when it changes
    private void renderMenu()
    {
        String mode = commandMode();

        if (!mode.equals(commandMode))
        {
            commandMode = mode;
            buildMenu();
        }
    }

    // What the menu depends on: if it is the same as before, it is the same
    private String commandMode()
    {
        if (presenter.isOver())
            return "over";

        if (presenter.isTargeting())
            return "target";

        if (presenter.isChoosing())
            return "choose:" + page + ":" + presenter.getAbilities().stream().map(a -> a.getName() + a.canPerform()).collect(Collectors.joining(","));

        return "play";
    }

    // There is no menu while the target is chosen: it is chosen on the battlefield (A / D, Enter)
    private void buildMenu()
    {
        menuBox.getChildren().clear();

        if (presenter.isOver())
            buildRetryMenu();
        else if (presenter.isTargeting())
            return;
        else if (presenter.isChoosing())
        {
            if (page.equals(PAGE_ABILITIES))
                buildAbilitiesMenu();
            else
                buildMainMenu();
        }
    }

    // The keys that work now, written in the bottom-left corner (there are no buttons for them)
    private String hint()
    {
        if (presenter.isOver())
            return "";

        if (presenter.isTargeting())
            return "A / D -- change target        ENTER -- confirm        ESC -- go back";

        if (presenter.isChoosing())
            return page.equals(PAGE_ABILITIES) ? "ESC -- go back" : "";

        return "E -- parry        Q -- dodge";
    }

    private void buildRetryMenu()
    {
        menuBox.getChildren().add(prompt("Retry battle?"));

        Button yes = menuButton("Yes   (Y)");
        yes.setOnAction(event -> startNewBattle());

        Button no = menuButton("No   (N)");
        no.setOnAction(event -> Platform.exit());

        menuBox.getChildren().addAll(yes, no);
    }

    // ATTACK / ABILITIES / ITEMS
    private void buildMainMenu()
    {
        Ability basic = basicAttack();

        Button attack = menuButton("Attack   (F)");
        attack.setDisable(basic == null || !basic.canPerform());
        attack.setOnAction(event -> presenter.chooseAbility(basic));

        Button abilities = menuButton("Abilities   (E)");
        abilities.setDisable(otherAbilities().isEmpty());
        abilities.setOnAction(event -> page = PAGE_ABILITIES);

        // There are no items in the game yet
        Button items = menuButton("Items   (W)");
        items.setDisable(true);
        items.setTooltip(new Tooltip("No items yet"));

        menuBox.getChildren().addAll(attack, abilities, items);
    }

    // The abilities in two columns of three: 1-3 on the left, 4-6 on the right
    private void buildAbilitiesMenu()
    {
        GridPane grid = new GridPane();
        grid.setHgap(ABILITY_GAP);
        grid.setVgap(ABILITY_GAP);

        List<Ability> abilities = otherAbilities();
        for (int i = 0; i < abilities.size(); i++)
        {
            Ability ability = abilities.get(i);
            Button button = abilityButton(i + 1, ability, abilities.size() > 3 ? ABILITY_WIDTH_NARROW : ABILITY_WIDTH);
            button.setDisable(!ability.canPerform());
            button.setOnAction(event -> presenter.chooseAbility(ability));
            grid.add(button, i / 3, i % 3);
        }

        menuBox.getChildren().add(grid);
    }


    // The name with its AP cost, and under it what the ability does. The size is fixed: a long description is cut
    // with "..." (the whole text is in the tooltip)
    private Button abilityButton(int number, Ability ability, double width)
    {
        // The text has room around it: the same on every side of the button, and the pointed ends are kept clear
        double left = 0.6 * bevel(ABILITY_HEIGHT) + 16;
        double right = 0.6 * bevel(ABILITY_HEIGHT) + 12;
        double textWidth = width - 2 * BORDER - left - right;

        Label name = new Label("[" + MENU_KEYS.get(number - 1).getName() + "]  " + ability.getName() + "   " +
                "(" + ability.getApCost() + ")");
        name.setStyle("-fx-text-fill: #f5ebd0; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label description = new Label(ability.getDescription());
        description.setStyle("-fx-text-fill: #b8b8cc; -fx-font-size: 11px;");
        description.setWrapText(true);
        description.setPrefWidth(textWidth);
        description.setMaxWidth(textWidth);
        description.setMinHeight(Region.USE_PREF_SIZE); // Forces JavaFX to allocate enough height for wrapped lines
        description.setAlignment(Pos.TOP_LEFT);       // two lines (the whole text is in the tooltip)

        // The text starts at the top of the button, always at the same height: the name is where the name is on every
        // button, whatever the length of the description is
        VBox text = new VBox(4, name, description);
        text.setAlignment(Pos.TOP_LEFT);

        return rpgButton(text, width, ABILITY_HEIGHT, Pos.TOP_LEFT, new Insets(8, right, 10, left));
    }

    private Button menuButton(String text)
    {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #f5ebd0; -fx-font-size: 14px; -fx-font-weight: bold;");

        return rpgButton(label, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT, Pos.CENTER);
    }

    /* *
        A button of the battle: an elongated hexagon, a thin silver border and a dark inside (see battle.css).
        It is two hexagons one in the other: the button itself is the gold one (its padding is the border) and
        what it holds, the inside, is a dark one a bit smaller. The stylesheet gives the colors, the hover and the
        disabled look; the shape is calculated here for the real size.
        If the stylesheet is missing the button is a plain dark box: the game works anyway.
    * */
    private Button rpgButton(Node content, double width, double height, Pos alignment)
    {
        double bevel = bevel(height);
        return rpgButton(content, width, height, alignment, new Insets(0, bevel * 0.6 + 8, 0, bevel * 0.6 + 14));
    }

    // padding: the room between the text and the border of the button
    private Button rpgButton(Node content, double width, double height, Pos alignment, Insets padding)
    {
        Button button = new Button();
        button.setFocusTraversable(false);       // the keys are the screen's, a focused button must not take them
        fixSize(button, width, height);

        StackPane inner = new StackPane(content);
        inner.setAlignment(alignment);
        inner.setPadding(padding);
        fixSize(inner, width - 2 * BORDER, height - 2 * BORDER);

        if (buttonStyleLoaded)
        {
            button.getStyleClass().add("rpg-button");
            button.setStyle(shape(width, height));
            inner.getStyleClass().add("rpg-button-inner");
            inner.setStyle(shape(width - 2 * BORDER, height - 2 * BORDER));
        }
        else
            button.setStyle(BUTTON_STYLE);

        button.setGraphic(inner);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        // A small press: the button gets a bit smaller while it is held down
        button.pressedProperty().addListener((observable, was, pressed) ->
        {
            button.setScaleX(pressed ? 0.98 : 1.0);
            button.setScaleY(pressed ? 0.98 : 1.0);
        });

        return button;
    }

    // How deep the slanted ends are: the same proportion for every height
    private static double bevel(double height)
    {
        return Math.round(height * 0.42);
    }

    // The style that gives a region the shape of an elongated hexagon of exactly this size
    private static String shape(double width, double height)
    {
        double bevel = bevel(height);

        return String.format(Locale.ROOT,
                "-fx-shape: \"M %1$.1f 0 L %2$.1f 0 L %3$.1f %4$.1f L %2$.1f %5$.1f L %1$.1f %5$.1f L 0 %4$.1f Z\";",
                bevel, width - bevel, width, height / 2, height);
    }

    private static void fixSize(Region region, double width, double height)
    {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
    }

    private Label prompt(String text)
    {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #ccccdd; -fx-font-size: 14px; -fx-effect: dropshadow(gaussian, black, 4, 0.8, 0, 0);");
        label.setWrapText(true);
        return label;
    }
}