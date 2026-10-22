package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyHeal;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnemyActionTest
{
    private final Enemy shaman = new Enemy("Shaman", new Stats(40, 3, 2, 3, 0));
    private final EnemyHeal mend = new EnemyHeal(shaman, "Mend", TargetType.SELF, 0.25);

    @Test void anActionKeepsItsAbilityAndTargets()
    {
        EnemyAction action = new EnemyAction(mend, List.of(shaman));

        assertSame(mend, action.ability());
        assertEquals(List.of(shaman), action.targets());
    }

    @Test void anActionNeedsAnAbilityAndTargets()
    {
        assertThrows(NullPointerException.class, () -> new EnemyAction(null, List.of(shaman)));
        assertThrows(NullPointerException.class, () -> new EnemyAction(mend, null));
        assertThrows(IllegalArgumentException.class, () -> new EnemyAction(mend, List.of()));
    }

    @Test void anActionKeepsItsOwnCopyOfTheTargets()
    {
        List<Fighter> targets = new ArrayList<>(List.of(shaman));
        EnemyAction action = new EnemyAction(mend, targets);

        targets.clear();

        assertEquals(1, action.targets().size());
    }
}