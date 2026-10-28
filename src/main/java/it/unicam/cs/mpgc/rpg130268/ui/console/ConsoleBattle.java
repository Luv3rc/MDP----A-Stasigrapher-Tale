package it.unicam.cs.mpgc.rpg130268.ui.console;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
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
import it.unicam.cs.mpgc.rpg130268.ui.InputLockout;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/* *
    The logic of the console UI for a Battle: it shows what happens, asks the player what to do, and punishes
    the keys that count for nothing (see InputLockout). It has no threads and no real time, it is driven by
        advance(deltaMs)   // time goes on
        input(line)        // the player typed a line (at the current time)
    so a test can play a whole fight. ConsoleBattleLoop drives it with the real time and the real keyboard.

    - The battle must be new: the runner counts the time from 0, like the battle clock
    - What happens is told when its time comes: the domain resolves a combo at once, but its events are stamped
      in the future, so they are kept and said when the clock gets there. Before every hit that can be reacted
      to, a cue says when the parry window opens
    - On the player's turn: the number of an ability, then (if it needs one) the number of the target
    - During an enemy attack: "p" parries and "d" dodges. A key that counts for nothing locks the keys for a while
* */
public class ConsoleBattle
{
    private record Line(long atMs, String text) {}

    private final Battle battle;
    private final ReactionWindows windows;
    private final InputLockout lockout;
    private final PrintStream out;

    private long timeMs = 0;
    private final List<Line> pending = new ArrayList<>();

    private boolean menuShown = false;
    private Ability chosenAbility = null;          // waiting for its target...
    private List<Fighter> candidates = List.of();  // ...to be chosen among these

    public ConsoleBattle(Battle battle, ReactionWindows windows, InputLockout lockout, PrintStream out)
    {
        this.battle = Objects.requireNonNull(battle, "battle must not be null");
        this.windows = Objects.requireNonNull(windows, "windows must not be null");
        this.lockout = Objects.requireNonNull(lockout, "lockout must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    // ---------- Driving it ----------

    public void start()
    {
        battle.start();
        collectEvents();
        flush();
    }

    // Time goes on: the battle moves and what has to be said by now is said
    public void advance(long deltaMs)
    {
        battle.update(deltaMs);
        timeMs += deltaMs;

        collectEvents();
        flush();
    }

    // The player typed a line (now)
    public void input(String text)
    {
        String line = text.trim().toLowerCase();
        if (line.isEmpty()) return;

        switch (battle.getPhase())
        {
            case WAITING_FOR_PLAYER -> choose(line);
            case PLAYING -> key(line);
            default -> { }
        }

        collectEvents();
        flush();
    }

    // Over, and everything has been said
    public boolean isOver() { return battle.getPhase() == BattlePhase.OVER && pending.isEmpty(); }

    // ---------- The keys ----------

    private void key(String line)
    {
        ReactionType type = switch (line)
        {
            case "e", "parry" -> ReactionType.PARRY;
            case "q", "dodge" -> ReactionType.DODGE;
            default -> null;
        };

        if (type == null)
        {
            out.println("(e = parry, q = dodge)");
            return;
        }

        // Nothing to react to (e.g. the player's own strikes are being played): the key is nothing
        if (!battle.isAttackInProgress()) return;

        // Locked: the key is ignored, the battle never hears about it
        if (lockout.isLocked(timeMs)) return;

        // The key counted for nothing (too early, too late...): the keys are locked for a while
        if (!battle.react(type))
        {
            lockout.punish(timeMs);
            out.println("   Missed! Your keys are locked for " + lockout.getPenaltyMs() + " ms.");
        }
    }

    // ---------- The choices of the player ----------

    private void choose(String line)
    {
        int number = number(line);
        Fighter actor = battle.getCurrentActor().orElseThrow();

        if (chosenAbility == null)
        {
            List<Ability> abilities = ((PartyMember) actor).getListOfAbility();

            if (number < 1 || number > abilities.size())
            {
                out.println("Type the number of one of the choices.");
                return;
            }

            chooseAbility(actor, abilities.get(number - 1));
        }
        else
        {
            if (number < 1 || number > candidates.size())
            {
                out.println("Type the number of one of the targets.");
                return;
            }

            Ability ability = chosenAbility;
            List<Fighter> target = List.of(candidates.get(number - 1));

            chosenAbility = null;
            perform(ability, target);
        }
    }

    // Abilities on one target ask which one (unless there is only one); the others know their targets
    private void chooseAbility(Fighter actor, Ability ability)
    {
        switch (ability.getTargetType())
        {
            case SELF -> perform(ability, List.of(actor));
            case ALL_ENEMIES -> perform(ability, battle.enemiesOf(actor));
            case ALL_ALLIES -> perform(ability, battle.alliesOf(actor));
            case ONE_ENEMY -> pickOne(ability, battle.enemiesOf(actor));
            case ONE_ALLY -> pickOne(ability, battle.alliesOf(actor));
        }
    }

    private void pickOne(Ability ability, List<Fighter> pool)
    {
        if (pool.size() == 1)
        {
            perform(ability, pool);
            return;
        }

        chosenAbility = ability;
        candidates = pool;

        out.println("On whom?");
        for (int i = 0; i < pool.size(); i++)
            out.println("  " + (i + 1) + ") " + describeFighter(pool.get(i)));
    }

    private void perform(Ability ability, List<Fighter> targets)
    {
        chosenAbility = null;

        try
        {
            battle.perform(ability, targets);
        }
        catch (IllegalStateException | IllegalArgumentException e)
        {
            // Nothing happened: it is still the turn of the player, so the menu comes back
            out.println("You can't do that: " + e.getMessage());
        }

        menuShown = false;
    }

    private static int number(String line)
    {
        try
        {
            return Integer.parseInt(line);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    // ---------- Saying what happens ----------

    private void collectEvents()
    {
        for (BattleEvent event : battle.drainEvents())
            describe(event);

        pending.sort(Comparator.comparingLong(Line::atMs));
    }

    // Says what is due by now. When everything has been said and it is the turn of the player: the menu
    private void flush()
    {
        while (!pending.isEmpty() && pending.getFirst().atMs() <= timeMs)
            out.println(pending.removeFirst().text());

        if (pending.isEmpty() && battle.getPhase() == BattlePhase.WAITING_FOR_PLAYER && !menuShown)
            showMenu();
    }

    private void say(long atMs, String text) { pending.add(new Line(atMs, text)); }

    private void describe(BattleEvent event)
    {
        switch (event)
        {
            case BattleEvent.TurnStarted e ->
                    say(e.atMs(), "-- Turn of " + e.actor().getName() + " --");

            case BattleEvent.Announced e ->
                    say(e.atMs(), e.text());

            case BattleEvent.Swing e -> { }          // only for the animations

            case BattleEvent.AttackStarted e ->
            {
                say(e.atMs(), e.attacker().getName() + " attacks " + names(e.targets()) + "!");
                scheduleCues(e);
            }

            case BattleEvent.HitResolved e ->
                    say(e.atMs(), hitText(e));

            case BattleEvent.Countered e ->
                    say(e.atMs(), e.defender().getName() + " counters " + e.attacker().getName()
                            + " for " + e.damage() + (e.critical() ? " (CRITICAL)" : "") + "!");

            case BattleEvent.Healed e ->
                    say(e.atMs(), e.target().getName() + " recovers " + e.amount() + " HP.");

            case BattleEvent.Defeated e ->
                    say(e.atMs(), e.fighter().getName() + " is defeated!");

            case BattleEvent.BattleEnded e ->
                    say(e.atMs(), endText(e));
        }
    }

    private String hitText(BattleEvent.HitResolved e)
    {
        String who = e.attacker().getName() + " -> " + e.target().getName() + ": ";

        if (e.reaction().isSuccessfulParry()) return who + "parried!";
        if (e.reaction().avoidsDamage()) return who + "dodged!";

        return who + e.damage() + " damage" + (e.critical() ? " (CRITICAL)" : "");
    }

    private String endText(BattleEvent.BattleEnded e)
    {
        return switch (e.outcome())
        {
            case VICTORY -> "VICTORY! Every survivor gains " + e.xp() + " XP.";
            case DEFEAT -> "DEFEAT... the party has fallen.";
            case ONGOING -> "The battle is over.";
        };
    }

    // A console has no animation: before every hit that can be reacted to, a cue says when the window opens
// A console has no animation: before every hit that can be reacted to, cues say when the windows open
    private void scheduleCues(BattleEvent.AttackStarted e)
    {
        long start = e.atMs();

        for (Hit hit : e.hits())
        {
            long impact = start + hit.windUpMs();
            ReactionOptions options = hit.options();

            if (options == ReactionOptions.DODGE_ONLY)
            {
                long dodgeStart = Math.max(start, impact - windows.earlyDodgeWindowMs());
                say(dodgeStart, "   >>> NOW! (q = dodge)");
            }
            else if (options == ReactionOptions.PARRY_ONLY)
            {
                long parryStart = Math.max(start, impact - windows.earlyParryWindowMs());
                say(parryStart, "   >>> NOW! (e = parry)");
            }
            else if (options == ReactionOptions.PARRY_OR_DODGE)
            {
                long dodgeStart = Math.max(start, impact - windows.earlyDodgeWindowMs());
                long parryStart = Math.max(start, impact - windows.earlyParryWindowMs());

                // The dodge window opens first
                say(dodgeStart, "   >>> NOW! (q = dodge)");

                // Then the parry window opens closer to the impact
                say(parryStart, "   >>> NOW! (e = parry, q = dodge)");
            }

            start = impact;
        }
    }

    private String cueText(ReactionOptions options)
    {
        return switch (options)
        {
            case PARRY_ONLY -> "(e = parry)";
            case DODGE_ONLY -> "(q = dodge)";
            default -> "(e = parry, q = dodge)";
        };
    }

    // ---------- The menu ----------

    private void showMenu()
    {
        menuShown = true;

        Fighter actor = battle.getCurrentActor().orElseThrow();

        out.println();
        for (Fighter ally : battle.alliesOf(actor))
            out.println("  " + describeFighter(ally));
        out.println("  vs");
        for (Fighter enemy : battle.enemiesOf(actor))
            out.println("  " + describeFighter(enemy));

        out.println("Your turn, " + actor.getName() + ". Choose an ability:");

        List<Ability> abilities = ((PartyMember) actor).getListOfAbility();
        for (int i = 0; i < abilities.size(); i++)
        {
            Ability ability = abilities.get(i);
            out.println("  " + (i + 1) + ") " + ability.getName() + " (AP " + ability.getApCost() + ")"
                    + (ability.canPerform() ? "" : "  [not available]"));
        }
    }

    private String describeFighter(Fighter fighter)
    {
        String text = fighter.getName() + "  HP " + fighter.getHp() + "/" + fighter.getStats().maxHealth();

        if (fighter instanceof PartyMember member)
            text += "  AP " + member.getAp().getCurrentAp() + "/" + member.getAp().getMaxAp();

        if (fighter instanceof Gunther gunther)
            text += "  INK " + gunther.getInk().getCurrentInk() + "/" + gunther.getInk().getMaxInk();

        return text;
    }

    private String names(List<Fighter> fighters)
    {
        return fighters.stream().map(Fighter::getName).collect(Collectors.joining(", "));
    }
}