package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.StasigraphicFury;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.ThirstForInk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuntherAbilitiesTest {
    private Gunther gunther(int startingAp) {
        return new Gunther(new Stats(50, 8, 4, 6, 3), 9, startingAp,
                ApRules.defaults(), InkRules.defaults());
    }

    private long countByName(Gunther g, String name) {
        return g.getAbilities().stream().filter(a -> a.getName().equals(name)).count();
    }

    @Test
    void learnedAbilityAppearsInList() {
        Gunther g = gunther(4);
        g.learn(ThirstForInk::new);
        assertEquals(1, countByName(g, "Thirst for Ink"));
    }

    @Test
    void learningTheSameAbilityTwiceDoesNotDuplicateIt() {
        Gunther g = gunther(4);
        g.learn(StasigraphicFury::new);
        int before = g.getAbilities().size();
        g.learn(StasigraphicFury::new);
        assertEquals(before, g.getAbilities().size(), "La dimensione della lista delle abilità non dovrebbe aumentare imparando un duplicato");
    }

    @Test
    void learnedAbilityIsBoundToGunther() {
        Gunther g = gunther(4);
        g.learn(StasigraphicFury::new);
        g.getInk().gainInk(4);

        Ability cuts = g.getAbilities().stream()
                .filter(a -> a.getName().equals("Stasigraphic Fury"))
                .findFirst().orElseThrow();

        assertTrue(cuts.canPerform());               // his AP and his ink are used
    }

    @Test
    void abilityListCannotBeModifiedFromOutside() {
        Gunther g = gunther(4);
        List<Ability> list = g.getAbilities();
        assertThrows(UnsupportedOperationException.class, () -> list.clear());
    }
}
