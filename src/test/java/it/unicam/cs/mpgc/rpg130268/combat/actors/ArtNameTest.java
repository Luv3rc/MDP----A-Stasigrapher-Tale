package it.unicam.cs.mpgc.rpg130268.combat.actors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArtNameTest
{
    private static Enemy enemy(String name) { return new Enemy(name, new Stats(10, 1, 1, 1, 0), 1, 0); }

    @Test void theArtNameIsTheNameUnlessItIsChanged()
    {
        assertEquals("Goblin", enemy("Goblin").getArtName());
    }

    @Test void anEnemyShownWithAnotherNameKeepsItsOwnPictures()
    {
        Enemy sariel = enemy("???").withArtName("sariel");

        assertEquals("???", sariel.getName());
        assertEquals("sariel", sariel.getArtName());
    }

    @Test void anArtNameIsNeverNull()
    {
        assertThrows(NullPointerException.class, () -> enemy("Goblin").withArtName(null));
    }
}
