package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurnOrderTest
{
    private Enemy fighter(String name, int agility, int luck) { return new Enemy(name, new Stats(40, 5, 2, agility, luck)); }

    private List<String> names(List<Fighter> fighters) { return fighters.stream().map(Fighter::getName).toList(); }

    @Test void theFastestGoesFirst()
    {
        List<Fighter> order = TurnOrder.sortByAgility(List.of(fighter("Slow", 2, 0), fighter("Fast", 9, 0), fighter("Mid", 5, 0)));

        assertEquals(List.of("Fast", "Mid", "Slow"), names(order));
    }

    @Test void withTheSameAgilityTheLuckiestGoesFirst()
    {
        List<Fighter> order = TurnOrder.sortByAgility(List.of(fighter("Unlucky", 5, 1), fighter("Lucky", 5, 8)));

        assertEquals(List.of("Lucky", "Unlucky"), names(order));
    }

    @Test void agilityCountsMoreThanLuck()
    {
        List<Fighter> order = TurnOrder.sortByAgility(List.of(fighter("LuckyButSlow", 3, 50), fighter("FastButUnlucky", 4, 0)));

        assertEquals(List.of("FastButUnlucky", "LuckyButSlow"), names(order));
    }

    @Test void withTheSameAgilityAndLuckTheOrderGivenIsKept()
    {
        // The list is built with the party first: that is how "party first" works
        List<Fighter> order = TurnOrder.sortByAgility(List.of(fighter("First", 5, 2), fighter("Second", 5, 2), fighter("Third", 5, 2)));

        assertEquals(List.of("First", "Second", "Third"), names(order));
    }

    @Test void theDeadDoNotTakeATurn()
    {
        Enemy dead = fighter("Dead", 9, 0);
        dead.setHp(0);

        List<Fighter> order = TurnOrder.sortByAgility(List.of(dead, fighter("Alive", 1, 0)));

        assertEquals(List.of("Alive"), names(order));
    }

    @Test void theGivenListIsNotChanged()
    {
        List<Fighter> given = List.of(fighter("Slow", 2, 0), fighter("Fast", 9, 0));

        TurnOrder.sortByAgility(given);

        assertEquals(List.of("Slow", "Fast"), names(given));
    }

    @Test void aMissingListThrows()
    {
        assertThrows(NullPointerException.class, () -> TurnOrder.sortByAgility(null));
    }
}