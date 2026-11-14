package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattlePhase;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/* *
    Everything a window needs to show a Battle, with no JavaFX in it
    The window only draws what the presenter says and tells it what the player did. It is driven by
    * advance(deltaMs)        // time goes on and returns the events that have just become visible
    * chooseAbility / chooseTarget / cancel / press   // what the player does

    - What the player SEES follows the battle clock, not the rules. The domain resolves a combo at once, but its
      events are stamped in the future. The presenter keeps them and shows each one when its time comes, and the
      HP bars (FighterView.getShownHp) change only then, so the bars, the numbers and the animations agree
    - The player can choose only when everything that happened has been shown (isChoosing())
    - A key that counts for nothing during an enemy attack locks the keys for a while (see InputLockout)
    - The cue says where the reaction windows of the next hit are, so the window can draw a timing bar

    * ChoiceMenu      the menu state machine (ability, then target)
    * ReactionInput   the reaction keys and the lockout
    * EventTimeline   the events, given back when their time comes
    * BattleLog       the text of the battle
    * ReactionCues    where the reaction windows are (the timing bar)
    * FighterView     a fighter as seen (the shown HP)
* */
public class BattlePresenter implements BattleChoices
{
    public static final String DEFAULT_AREA = "default";

    private final Battle battle;
    private final EventTimeline timeline;
    private final ChoiceMenu menu;
    private final ReactionInput input;
    private final BattleLog log = new BattleLog();
    private final ReactionCues cues;

    private final List<FighterView> party = new ArrayList<>();
    private final List<FighterView> enemies = new ArrayList<>();
    private final Map<Fighter, FighterView> views = new HashMap<>();

    private long timeMs = 0;
    private String area = DEFAULT_AREA;             // where the battle takes place (it only decides the background)
    private BattleEvent.BattleEnded end = null;

    public BattlePresenter(Battle battle, List<PartyMember> party, List<Enemy> enemies,
                           ReactionWindows windows, InputLockout lockout)
    {
        this.battle = Objects.requireNonNull(battle, "battle must not be null");
        Objects.requireNonNull(windows, "windows must not be null");
        Objects.requireNonNull(lockout, "lockout must not be null");

        this.timeline = new EventTimeline(battle);
        this.menu = new ChoiceMenu(battle, this::perform);
        this.input = new ReactionInput(battle, lockout, log);
        this.cues = new ReactionCues(windows);

        for (PartyMember member : Objects.requireNonNull(party, "party must not be null"))
            register(member, this.party);

        for (Enemy enemy : Objects.requireNonNull(enemies, "enemies must not be null"))
            register(enemy, this.enemies);
    }

    private void register(Fighter fighter, List<FighterView> side)
    {
        FighterView view = new FighterView(fighter);
        side.add(view);
        views.put(fighter, view);
    }


    // ---------- Driving it ----------

    public void start()
    {
        battle.start();
        timeline.collect();
    }

    // Time goes on. Returns the events that have just become visible (the window animates them)
    public List<BattleEvent> advance(long deltaMs)
    {
        battle.update(deltaMs);
        timeMs += deltaMs;

        timeline.collect();
        List<BattleEvent> shown = timeline.releaseUpTo(timeMs);
        shown.forEach(this::show);

        cues.forgetClosed(timeMs);
        return shown;
    }

    public long getTimeMs() { return timeMs; }

    // Where the battle takes place ("forest", "castle"...). The game does not care: it is the name the screen uses
    // to choose the background. Without it, DEFAULT_AREA
    public String getArea() { return area; }

    public void setArea(String area)
    {
        this.area = (area == null || area.isBlank()) ? DEFAULT_AREA : area.strip();
    }

    // By default a single possible target is chosen at once. With true the player is always asked (to confirm it)
    public void setAlwaysAskTarget(boolean alwaysAskTarget) { menu.setAlwaysAskTarget(alwaysAskTarget); }


    // ---------- What the player does ----------

    @Override
    public void chooseAbility(Ability ability)
    {
        Objects.requireNonNull(ability, "ability must not be null");

        if (!isChoosing()) return;

        menu.chooseAbility(ability);
    }

    @Override
    public void chooseTarget(Fighter target) { menu.chooseTarget(target); }

    @Override
    public void cancel() { menu.cancel(); }

    // A key of the player (now). It counts only while an enemy attack is being played
    public void press(ReactionType type) { input.press(type, timeMs); }

    private void perform(Ability ability, List<Fighter> targets)
    {
        try
        {
            battle.perform(ability, targets);
            timeline.collect();
        }
        catch (IllegalStateException | IllegalArgumentException e)
        {
            // Nothing happened (golden rule), it is still the turn of the player
            log.add("You can't do that: " + e.getMessage());
        }
    }


    // ---------- What the window shows ----------

    public List<FighterView> getParty() { return List.copyOf(party); }

    public List<FighterView> getEnemies() { return List.copyOf(enemies); }

    public BattlePhase getPhase() { return battle.getPhase(); }

    public int getRound() { return battle.getRound(); }

    public Optional<Fighter> getCurrentActor() { return battle.getCurrentActor(); }

    @Override
    public boolean isChoosing() { return battle.getPhase() == BattlePhase.WAITING_FOR_PLAYER && timeline.isEmpty(); }

    @Override
    public List<Ability> getAbilities()
    {
        if (!isChoosing()) return List.of();

        return ((PartyMember) battle.getCurrentActor().orElseThrow()).getListOfAbility();
    }

    @Override
    public boolean isTargeting() { return menu.isTargeting(); }

    @Override
    public boolean isMenuOpen() { return isChoosing() && !isTargeting(); }

    @Override
    public List<Fighter> getTargetCandidates() { return menu.candidates(); }

    public boolean isLocked() { return input.isLocked(timeMs); }

    public long getLockedRemainingMs() { return input.remainingMs(timeMs); }

    // What the window can say, in order (the whole battle so far)
    public List<String> getLog() { return log.lines(); }

    // The next hit that can be reacted to, if it has started
    public Optional<Cue> getCue() { return cues.cueAt(timeMs); }

    // Over, and everything has been shown
    @Override
    public boolean isOver() { return end != null; }

    public Optional<BattleEvent.BattleEnded> getEnd() { return Optional.ofNullable(end); }


    // ---------- Inside ----------

    // What an event changes in what the player sees
    private void show(BattleEvent event)
    {
        switch (event)
        {
            case BattleEvent.TurnStarted e -> { }
            case BattleEvent.Announced e -> { }
            case BattleEvent.Swing e -> { }
            case BattleEvent.AttackStarted e -> cues.remember(e);
            case BattleEvent.HitResolved e -> view(e.target()).damage(e.damage());
            case BattleEvent.Countered e -> view(e.attacker()).damage(e.damage());
            case BattleEvent.Healed e -> view(e.target()).heal(e.amount());
            case BattleEvent.Defeated e -> view(e.fighter()).fall();
            case BattleEvent.BattleEnded e -> end = e;
        }

        log.record(event);
    }

    private FighterView view(Fighter fighter)
    {
        FighterView view = views.get(fighter);

        if (view == null)
            throw new IllegalStateException(fighter.getName() + " is not part of this battle");

        return view;
    }
}
