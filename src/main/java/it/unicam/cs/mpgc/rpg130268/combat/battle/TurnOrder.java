package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class TurnOrder
{
    // Sorts a list of living fighters by Agility in descending order (highest speed goes first)
    public static List<Fighter> sortByAgility(List<Fighter> fighters)
    {
        Objects.requireNonNull(fighters, "fighters must not be null");
        return fighters.stream()
                .filter(Fighter::isAlive)
                .sorted(Comparator.comparingInt((Fighter f) -> f.getStats().agility()).reversed())
                .toList();
    }
}
