package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final class BattleLog
{
    private final List<String> lines = new ArrayList<>();

    void add(String line) { lines.add(Objects.requireNonNull(line, "line must not be null")); }

    void record(BattleEvent event)
    {
        String text = textOf(event);

        if (!text.isEmpty()) lines.add(text);
    }

    List<String> lines() { return List.copyOf(lines); }

    static String textOf(BattleEvent event)
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
                case VICTORY -> "VICTORY! The party gains " + e.totalXp() + " XP.";
                case DEFEAT -> "DEFEAT... the party has fallen.";
                case ONGOING -> "The battle is over.";
            };
        };
    }

    private static String hitText(BattleEvent.HitResolved e)
    {
        String who = e.attacker().getName() + " -> " + e.target().getName() + ": ";

        if (e.reaction().isSuccessfulParry()) return who + "parried!";
        if (e.reaction().avoidsDamage()) return who + "dodged!";

        return who + e.damage() + " damage" + (e.critical() ? " (CRITICAL)" : "");
    }

    private static String names(List<Fighter> fighters)
    {
        return fighters.stream().map(Fighter::getName).collect(Collectors.joining(", "));
    }
}
