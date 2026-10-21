package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class TurnOrder
{
    // Sorts a list of living fighters by Agility in descending order (highest speed goes first)
    // If tie on Agility, then Luck will be the new sorter
    // If tie on Agility and Luck, then party first
    public static List<Fighter> sortByAgility(List<Fighter> fighters)
    {
        Objects.requireNonNull(fighters, "fighters must not be null");

        Comparator<Fighter> byAgility = Comparator.comparingInt((Fighter f) -> f.getStats().agility()).reversed();
        Comparator<Fighter> byLuck    = Comparator.comparingInt((Fighter f) -> f.getStats().luck()).reversed();

        return fighters.stream()
                .filter(Fighter::isAlive)
                .sorted(byAgility.thenComparing(byLuck))
                .toList();
    }
}
