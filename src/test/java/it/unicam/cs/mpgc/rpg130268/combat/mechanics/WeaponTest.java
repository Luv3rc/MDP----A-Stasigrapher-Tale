package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

import it.unicam.cs.mpgc.rpg130268.combat.damage.Scaling;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeaponTest
{
    private static final Scaling STRENGTH_B = Scaling.of(ScalingStat.STRENGTH, ScalingGrade.B);

    @Test void aWeaponHasAnIdAndAName()
    {
        Weapon weapon = new Weapon("thesmosia", "Thesmosia", WeaponType.SWORD, 20, STRENGTH_B);

        assertEquals("thesmosia", weapon.id());
        assertEquals("Thesmosia", weapon.name());
    }

    @Test void theNameCanChangeWithoutTheIdChanging()
    {
        Weapon first = new Weapon("thesmosia", "Thesmosia", WeaponType.SWORD, 20, STRENGTH_B);
        Weapon renamed = new Weapon("thesmosia", "The Chained Blade", WeaponType.SWORD, 20, STRENGTH_B);

        assertEquals(first.id(), renamed.id());
        assertNotEquals(first.name(), renamed.name());
    }

    @Test void withoutAnIdTheNameGivesIt()
    {
        assertEquals("iron-broadsword", new Weapon("Iron Broadsword", WeaponType.SWORD, 15, ScalingGrade.C).id());
        assertEquals("assassin-s-dagger", new Weapon("Assassin's Dagger", WeaponType.DAGGER, 8, ScalingGrade.D, ScalingStat.AGILITY, ScalingGrade.A).id());
        assertEquals("unarmed", Weapon.unarmed().id());
    }

    @Test void aBadIdIsRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> new Weapon("Thesmosia", "Thesmosia", WeaponType.SWORD, 20, STRENGTH_B));
        assertThrows(IllegalArgumentException.class, () -> new Weapon("", "Thesmosia", WeaponType.SWORD, 20, STRENGTH_B));
        assertThrows(NullPointerException.class, () -> new Weapon(null, "Thesmosia", WeaponType.SWORD, 20, STRENGTH_B));
    }
}
