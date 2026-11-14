package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenter;
import it.unicam.cs.mpgc.rpg130268.ui.FighterView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

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

    BattleLayout       where everything is (and the texts, the log and the timing bar)
    BattleLoop         the frames
    BattleKeys         what every key does (and the keys that work now)
    EndOfBattle        self-explanatory
    BattleEnding       self-explanatory
    BattleBackground   the picture of the area, darkened
    EnemyFormation     where the enemies stand
    FighterCard        one sprite with its life bar (one for every fighter)
    PartyPanel         the HUD of a party member (face, life, AP, ink)
    BattleMenu         ATTACK / ABILITIES / ITEMS
    TargetSelection    A / D / Enter: which target is selected
    BattleAnimator     what the events of the battle look like (cuts, numbers, shakes)
    BattleOverlay      the announcement of an ability and the banner of the end
    RpgButtons         the hexagon buttons (css/battle.css, in the resources)

    How it works:
    - The loop calls frame() every frame: the presenter returns the events that have just become visible and they
      are animated, then everything is drawn again
    - A key is stamped where it happened: before it is handled the battle is moved forward up to that moment,
      not only up to the last frame, so the timing of the player counts exactly
    - The keys that can be used now are written in the bottom-left corner (ESC -- go back, E -- parry...):
      there are no buttons for them
    - Keys: F, E, W = the first, second, third entry of the menu, Esc = back, E = parry, Q = dodge (the menu and the
      attacks never happen together), A / D (or the arrows) and Enter = the target, Y / N = retry the battle or not (A / D and Enter in the question),
      T = show/hide the timing bar, L = show/hide the log
    - The pictures of the background and of the characters are looked for in the resources, see SpriteArt.
      Without them the game draws plain shapes
* */
public class BattleScreen implements GameScreen
{
    private final Supplier<BattlePresenter> factory;

    private final BattleOverlay overlay = new BattleOverlay();
    private final EnemyFormation enemies = new EnemyFormation();
    private final Map<Fighter, FighterCard> cards = new LinkedHashMap<>();
    private final List<PartyPanel> panels = new ArrayList<>();

    private final BattleEnding ending;
    private final BattleMenu menu;
    private final TargetSelection targeting;
    private final BattleLayout layout;
    private final BattleBackground background;
    private final BattleAnimator animator;
    private final BattleKeys keys;
    private final EndOfBattle endOfBattle;
    private final BattleLoop loop = new BattleLoop(this::advance, this::render, this::afterFrame);

    private BattlePresenter presenter;
    private int logShown;

    // A battle on its own: at the end it asks to retry
    public BattleScreen(Supplier<BattlePresenter> factory)
    {
        this(factory, null);
    }

    // A battle of the story. When it ends, after a moment, the exit is told how it went
    BattleScreen(Supplier<BattlePresenter> factory, BattleExit exit)
    {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");

        // The look of the buttons (src/main/resources/css/battle.css). If it is not there the buttons are plain
        URL stylesheet = BattleScreen.class.getResource("/css/battle.css");
        boolean styled = stylesheet != null;

        ending = exit == null ? new BattleRetry(new RpgButtons(styled), this::startNewBattle) : BattleEnding.story(exit);
        overlay.setOutcomeBanner(ending.showsOutcomeBanner());

        // The pieces that need "the battle of now" get a way to ask for it: a new presenter is made at every battle
        menu = new BattleMenu(new RpgButtons(styled), () -> presenter);
        targeting = new TargetSelection(cards, () -> presenter);

        layout = new BattleLayout(enemies.getRoot(), menu.getRoot(), overlay);
        if (styled) layout.addStylesheet(stylesheet);

        // The background is the first thing of the window, under everything. Which picture it is depends on the area
        // of the battle (see startNewBattle)
        background = new BattleBackground(layout.stage(), layout.content());
        layout.addBehind(background.nodes());
        ending.popup().ifPresent(layout::addOnTop);      // the question is over everything

        animator = new BattleAnimator(cards, new SlashEffect(layout.slashLayer()), overlay, () -> presenter.getTimeMs());
        keys = new BattleKeys(() -> presenter, menu, targeting, ending,
                layout::toggleTimingBar, layout::toggleLog, loop::catchUp);
        endOfBattle = new EndOfBattle(ending);
    }

    @Override
    public Region getRoot() { return layout.stage(); }

    @Override
    public void onShow() { startNewBattle(); }

    // A new fight: the presenter is built again, and the sprites, the HUD, the log and the menu with it
    public void startNewBattle()
    {
        loop.stop();

        presenter = factory.get();
        endOfBattle.reset();
        ending.reset();

        presenter.setAlwaysAskTarget(true);     // even with a single enemy, the player confirms it
        presenter.start();

        for (FighterCard old : cards.values()) old.dispose();
        cards.clear();
        panels.clear();
        layout.clearFighters();
        enemies.clear();

        for (FighterView view : presenter.getParty())
        {
            PartyPanel panel = new PartyPanel(view);
            panels.add(panel);
            layout.addPartyMember(makeCard(view).getRoot(), panel.getRoot());
        }

        List<FighterCard> enemyCards = new ArrayList<>();
        for (FighterView view : presenter.getEnemies()) enemyCards.add(makeCard(view));
        enemies.place(enemyCards);

        background.show(presenter.getArea());
        animator.reset();
        menu.reset();

        layout.clearLog();
        logShown = 0;

        loop.start();
    }

    @Override
    public void stop()
    {
        loop.stop();
        for (FighterCard card : cards.values()) card.dispose();
    }

    @Override
    public void onKeyPressed(KeyEvent event) { keys.onKey(event.getCode()); }

    private FighterCard makeCard(FighterView view)
    {
        FighterCard card = new FighterCard(view);

        // Clicking a sprite chooses it as the target (the presenter ignores it if nothing is waiting for one)
        card.getRoot().setOnMouseClicked(event -> presenter.chooseTarget(view.getFighter()));

        cards.put(view.getFighter(), card);
        return card;
    }

    // ---------- Every frame ----------

    // Time goes on: what has just become visible is animated
    private void advance(long deltaMs)
    {
        presenter.advance(deltaMs).forEach(animator::animate);
    }

    private void afterFrame(long nowNanos) { endOfBattle.check(presenter, nowNanos); }

    private void render()
    {
        String actor = presenter.getCurrentActor().map(Fighter::getName).orElse("-");
        layout.setTitle("Round " + presenter.getRound() + "   |   " + actor + (presenter.isChoosing() ? "   (your turn)" : ""));
        layout.setLock(presenter.isLocked() ? "KEYS LOCKED  " + presenter.getLockedRemainingMs() + " ms" : "");

        if (!presenter.isTargeting()) targeting.reset();

        List<Fighter> candidates = presenter.isTargeting() ? targeting.candidates() : List.of();
        Fighter selected = targeting.selected(candidates);

        for (Map.Entry<Fighter, FighterCard> entry : cards.entrySet())
        {
            Fighter fighter = entry.getKey();
            boolean active = presenter.getCurrentActor().isPresent() && presenter.getCurrentActor().get() == fighter;

            entry.getValue().update(active, candidates.contains(fighter), fighter == selected);
        }

        layout.setHint(keys.hint());

        for (PartyPanel panel : panels) panel.update();

        layout.showCue(presenter.getCue());
        animator.update();

        List<String> log = presenter.getLog();
        for (; logShown < log.size(); logShown++)
            layout.appendLog(log.get(logShown));

        menu.update();
    }
}
