package it.unicam.cs.mpgc.rpg130268.combat.mechanics;

import it.unicam.cs.mpgc.rpg130268.combat.damage.Scaling;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import java.util.Objects;

public record Weapon(String name, WeaponType type, int baseDamage, Scaling scaling)
{
    public Weapon
    {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "weaponType must not be null");
        Objects.requireNonNull(scaling, "scaling must not be null");
        if (baseDamage < 0)
            throw new IllegalArgumentException("baseDamage must be non-negative");
    }

    // Weapons with no scaling at all (Only base one that's strength)
    // * new Weapon("Iron Broadsword", WeaponType.SWORD, 15, ScalingGrade.C)
    public Weapon(String name, WeaponType type, int baseDamage, ScalingGrade strengthGrade) {
        this(name, type, baseDamage, Scaling.of(ScalingStat.STRENGTH, strengthGrade));
    }

    // Weapons with scaling other than strenght
    // * new Weapon("Assassin's Dagger", WeaponType.DAGGER, 8, ScalingGrade.D, ScalingStat.AGILITY, ScalingGrade.A)
    public Weapon(String name, WeaponType type, int baseDamage,
                  ScalingGrade strengthGrade, ScalingStat extraStat,
                  ScalingGrade extraGrade)
    {
        this(name, type, baseDamage, Scaling.of(ScalingStat.STRENGTH, strengthGrade).and(extraStat, extraGrade));
    }

    public static Weapon unarmed() {
        return new Weapon("Unarmed", WeaponType.UNARMED, 1, Scaling.of(ScalingStat.STRENGTH, ScalingGrade.D));
    }
}