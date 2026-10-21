package it.unicam.cs.mpgc.rpg130268.combat.actors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnemyTest
{
    private final Stats stats = new Stats(40, 5, 2, 3, 0);

    @Test void anEnemyKnowsItsLevelAndItsXpReward()
    {
        Enemy ogre = new Enemy("Ogre", stats, 12, 340);

        assertEquals(12, ogre.getLevel());
        assertEquals(340, ogre.getXpReward());
    }

    @Test void withoutALevelAnEnemyIsLevelOneWithNoReward()
    {
        Enemy placeholder = new Enemy("Placeholder", stats);

        assertEquals(1, placeholder.getLevel());
        assertEquals(0, placeholder.getXpReward());
    }

    @Test void levelAndRewardAreValidated()
    {
        assertThrows(IllegalArgumentException.class, () -> new Enemy("Bad", stats, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new Enemy("Bad", stats, 1, -1));
        assertDoesNotThrow(() -> new Enemy("Harmless", stats, 1, 0));
    }

    @Test void anEnemyCanWieldAnyKindOfWeapon()
    {
        for (WeaponType type : WeaponType.values())
        {
            Enemy enemy = new Enemy("Goblin", stats, 3, 30);
            Weapon weapon = new Weapon("A " + type, type, 10, ScalingGrade.C);

            enemy.equip(weapon);

            assertEquals(weapon, enemy.getWeapon());
        }
    }

    @Test void anEnemyStartsUnarmed()
    {
        assertEquals(WeaponType.UNARMED, new Enemy("Goblin", stats, 3, 30).getWeapon().type());
    }
}