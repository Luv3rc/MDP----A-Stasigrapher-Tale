package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattlePhase;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/* *
    Everything a window needs to show a Battle, with no JavaFX in it
    The window only draws what the presenter says and tells it what the player did. It is driven by
        advance(deltaMs)        // time goes on and returns the events that have just become visible
        chooseAbility / chooseTarget / cancel / press   // what the player does

    - What the player SEES follows the battle clock, not the rules. The domain resolves a combo at once, but its
      events are stamped in the future. The presenter keeps them and shows each one when its time comes, and the
      HP bars (FighterView.getShownHp) change only then, so the bars, the numbers and the animations agree
    - The player can choose only when everything that happened has been shown (isChoosing())
    - A key that counts for nothing during an enemy attack locks the keys for a while (see InputLockout)
    - The cue says where the reaction windows of the next hit are, so the window can draw a timing bar
* */
public class BattlePresenter
{
    public static final String DEFAULT_AREA = "default";

    // A fighter as the player sees it. the HP bar follows what has been SHOWN, not the real HP
    public static class FighterView
    {
        private final Fighter fighter;
        private final int maxHp;
        private int shownHp;

        FighterView(Fighter fighter)
        {
            this.fighter = fighter;
            this.maxHp = fighter.getStats().maxHealth();
            this.shownHp = fighter.getHp();
        }

        public Fighter getFighter() { return fighter; }
        public String getName() { return fighter.getName(); }
        public int getMaxHp() { return maxHp; }
        public int getShownHp() { return shownHp; }
        public boolean isAlive() { return shownHp > 0; }
        public boolean isPartyMember() { return fighter instanceof PartyMember; }

        // Only the party has AP; this is the real value, which changes at once
        public int getAp() { return fighter instanceof PartyMember member ? member.getAp().getCurrentAp() : 0; }
        public int getMaxAp() { return fighter instanceof PartyMember member ? member.getAp().getMaxAp() : 0; }

        // Inside BattlePresenter.FighterView class:
        public int getInk()
        {
            // Check if the fighter is Gunther or has an Ink pool mechanism
            if (fighter instanceof Gunther gunther) {
                return gunther.getInk().getCurrentInk(); // Adjust to match your actual Ink method name (e.g., getCurrentAmount() or similar)
            }
            return 0;
        }

        public int getMaxInk()
        {
            if (fighter instanceof Gunther gunther) {
                return gunther.getInk().getMaxInk(); // Adjust to match your max ink method name
            }
            return 0;
        }

        public int getLevel()
        {
            if (fighter instanceof PartyMember member) return member.getLevel();
            if (fighter instanceof Enemy enemy) return enemy.getLevel();
            return 1;
        }

        void damage(int amount) { shownHp = Math.max(0, shownHp - amount); }
        void heal(int amount) { shownHp = Math.min(maxHp, shownHp + amount); }
        void fall() { shownHp = 0; }
    }

    // Where the reaction windows of the next hit are, on the battle clock, for drawing a timing bar.
    // The parry (or dodge) is good from ...FromMs to ...ToMs, and the hit lands at impactMs
    public record Cue(long nowMs, long startMs, long impactMs,
                      boolean parry, long parryFromMs, long parryToMs,
                      boolean dodge, long dodgeFromMs, long dodgeToMs) {}

    private record TimedHit(long startMs, long impactMs, ReactionOptions options) {}

    private final Battle battle;
    private final ReactionWindows windows;
    private final InputLockout lockout;

    private final List<FighterView> party = new ArrayList<>();
    private final List<FighterView> enemies = new ArrayList<>();
    private final Map<Fighter, FighterView> views = new HashMap<>();

    private final List<BattleEvent> pending = new ArrayList<>();
    private final List<TimedHit> hitsInProgress = new ArrayList<>();
    private final List<String> log = new ArrayList<>();

    private long timeMs = 0;
    private String area = DEFAULT_AREA;             // where the battle takes place (it only decides the background)
    private boolean alwaysAskTarget = false;        // ask for the target even if only one can be chosen
    private Ability chosenAbility = null;           // waiting for its target...
    private List<Fighter> candidates = List.of();   // ...to be chosen among these
    private BattleEvent.BattleEnded end = null;

    public BattlePresenter(Battle battle, List<PartyMember> party, List<Enemy> enemies,
                           ReactionWindows windows, InputLockout lockout)
    {
        this.battle = Objects.requireNonNull(battle, "battle must not be null");
        this.windows = Objects.requireNonNull(windows, "windows must not be null");
        this.lockout = Objects.requireNonNull(lockout, "lockout must not be null");

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
        collect();
    }

    // Time goes on. Returns the events that have just become visible (the window animates them)
    public List<BattleEvent> advance(long deltaMs)
    {
        battle.update(deltaMs);
        timeMs += deltaMs;

        collect();
        return release();
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
    public void setAlwaysAskTarget(boolean alwaysAskTarget) { this.alwaysAskTarget = alwaysAskTarget; }


    // ---------- What the player does ----------

    // The player chose an ability. If it needs one target among several, the choice of the target comes next
    public void chooseAbility(Ability ability)
    {
        Objects.requireNonNull(ability, "ability must not be null");

        if (!isChoosing()) return;

        Fighter actor = battle.getCurrentActor().orElseThrow();

        switch (ability.getTargetType())
        {
            case SELF -> perform(ability, List.of(actor));
            case ALL_ENEMIES -> perform(ability, battle.enemiesOf(actor));
            case ALL_ALLIES -> perform(ability, battle.alliesOf(actor));
            case ONE_ENEMY -> pickOne(ability, battle.enemiesOf(actor));
            case ONE_ALLY -> pickOne(ability, battle.alliesOf(actor));
        }
    }

    public void chooseTarget(Fighter target)
    {
        if (chosenAbility == null || !candidates.contains(target)) return;

        perform(chosenAbility, List.of(target));
    }

    // Changes of mind: back to the abilities
    public void cancel()
    {
        chosenAbility = null;
        candidates = List.of();
    }

    // A key of the player (now). It counts only while an enemy attack is being played
    public void press(ReactionType type)
    {
        Objects.requireNonNull(type, "type must not be null");

        // Nothing to react to (e.g. the player's own strikes are being played): the key is nothing
        if (!battle.isAttackInProgress()) return;

        // Locked: the key is ignored, the battle never hears about it
        if (lockout.isLocked(timeMs)) return;

        // The key counted for nothing (too early, too late...): the keys are locked for a while
        if (!battle.react(type))
        {
            lockout.punish(timeMs);
            log.add("   Missed! Your keys are locked for " + lockout.getPenaltyMs() + " ms.");
        }
    }

    private void pickOne(Ability ability, List<Fighter> pool)
    {
        if (pool.size() == 1 && !alwaysAskTarget)
        {
            perform(ability, pool);
            return;
        }

        chosenAbility = ability;
        candidates = List.copyOf(pool);
    }

    private void perform(Ability ability, List<Fighter> targets)
    {
        cancel();

        try
        {
            battle.perform(ability, targets);
            collect();
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

    // The player can choose now
    public boolean isChoosing() { return battle.getPhase() == BattlePhase.WAITING_FOR_PLAYER && pending.isEmpty(); }

    public List<Ability> getAbilities()
    {
        if (!isChoosing()) return List.of();

        return ((PartyMember) battle.getCurrentActor().orElseThrow()).getListOfAbility();
    }

    // An ability has been chosen and it needs a target among these
    public boolean isTargeting() { return chosenAbility != null; }

    public List<Fighter> getTargetCandidates() { return candidates; }

    public boolean isLocked() { return lockout.isLocked(timeMs); }

    public long getLockedRemainingMs() { return lockout.remainingMs(timeMs); }

    // What the window can say, in order (the whole battle so far)
    public List<String> getLog() { return List.copyOf(log); }

    // The next hit that can be reacted to, if it has started
    public Optional<Cue> getCue()
    {
        for (TimedHit hit : hitsInProgress)
        {
            if (hit.options() == ReactionOptions.NONE) continue;
            if (timeMs < hit.startMs()) return Optional.empty();

            return Optional.of(new Cue(timeMs, hit.startMs(), hit.impactMs(),
                    hit.options().allows(ReactionType.PARRY),
                    hit.impactMs() - windows.earlyParryWindowMs(), hit.impactMs() + windows.lateParryWindowMs(),
                    hit.options().allows(ReactionType.DODGE),
                    hit.impactMs() - windows.earlyDodgeWindowMs(), hit.impactMs() + windows.lateDodgeWindowMs()));
        }

        return Optional.empty();
    }

    // Over, and everything has been shown
    public boolean isOver() { return end != null; }

    public Optional<BattleEvent.BattleEnded> getEnd() { return Optional.ofNullable(end); }

    // ---------- Inside ----------

    private void collect()
    {
        pending.addAll(battle.drainEvents());
        pending.sort(Comparator.comparingLong(BattleEvent::atMs));
    }

    private List<BattleEvent> release()
    {
        List<BattleEvent> shown = new ArrayList<>();

        while (!pending.isEmpty() && pending.getFirst().atMs() <= timeMs)
        {
            BattleEvent event = pending.removeFirst();
            show(event);
            shown.add(event);
        }

        // The hits whose reaction windows are closed are not needed any more
        hitsInProgress.removeIf(hit -> timeMs > hit.impactMs() + lateMaxMs(hit.options()));

        return shown;
    }

    // What an event changes in what the player sees
    private void show(BattleEvent event)
    {
        switch (event)
        {
            case BattleEvent.TurnStarted e -> { }
            case BattleEvent.Announced e -> { }
            case BattleEvent.Swing e -> { }
            case BattleEvent.AttackStarted e -> rememberHits(e);
            case BattleEvent.HitResolved e -> view(e.target()).damage(e.damage());
            case BattleEvent.Countered e -> view(e.attacker()).damage(e.damage());
            case BattleEvent.Healed e -> view(e.target()).heal(e.amount());
            case BattleEvent.Defeated e -> view(e.fighter()).fall();
            case BattleEvent.BattleEnded e -> end = e;
        }

        String text = textOf(event);
        if (!text.isEmpty()) log.add(text);          // some events are only for the animations
    }

    private void rememberHits(BattleEvent.AttackStarted event)
    {
        long start = event.atMs();

        for (Hit hit : event.hits())
        {
            long impact = start + hit.windUpMs();
            hitsInProgress.add(new TimedHit(start, impact, hit.options()));
            start = impact;
        }
    }

    private FighterView view(Fighter fighter)
    {
        FighterView view = views.get(fighter);

        if (view == null)
            throw new IllegalStateException(fighter.getName() + " is not part of this battle");

        return view;
    }

    // How late a reaction can still be good for a hit with these options
    private long lateMaxMs(ReactionOptions options)
    {
        return switch (options)
        {
            case NONE -> 0;
            case PARRY_ONLY -> windows.lateParryWindowMs();
            case DODGE_ONLY -> windows.lateDodgeWindowMs();
            case PARRY_OR_DODGE -> Math.max(windows.lateParryWindowMs(), windows.lateDodgeWindowMs());
        };
    }

    private String textOf(BattleEvent event)
    {
        return switch (event)
        {
            case BattleEvent.TurnStarted e -> "-- Turn of " + e.actor().getName() + " --";
            case BattleEvent.Announced e -> e.text();
            case BattleEvent.Swing e -> "";
            case BattleEvent.AttackStarted e -> e.attacker().getName() + " attacks " + names(e.targets()) + "!";
            case BattleEvent.HitResolved e -> hitText(e);
            case BattleEvent.Countered e -> e.defender().getName() + " counters " + e.attacker().getName()
                    + " for " + e.damage() + (e.critical() ? " (CRITICAL)" : "") + "!";
            case BattleEvent.Healed e -> e.target().getName() + " recovers " + e.amount() + " HP.";
            case BattleEvent.Defeated e -> e.fighter().getName() + " is defeated!";
            case BattleEvent.BattleEnded e -> switch (e.outcome())
            {
                case VICTORY -> "VICTORY! Every survivor gains " + e.xp() + " XP.";
                case DEFEAT -> "DEFEAT... the party has fallen.";
                case ONGOING -> "The battle is over.";
            };
        };
    }

    private String hitText(BattleEvent.HitResolved e)
    {
        String who = e.attacker().getName() + " -> " + e.target().getName() + ": ";

        if (e.reaction().isSuccessfulParry()) return who + "parried!";
        if (e.reaction().avoidsDamage()) return who + "dodged!";

        return who + e.damage() + " damage" + (e.critical() ? " (CRITICAL)" : "");
    }

    private String names(List<Fighter> fighters)
    {
        return fighters.stream().map(Fighter::getName).collect(Collectors.joining(", "));
    }
}