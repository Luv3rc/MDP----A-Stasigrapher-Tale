package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.damage.Scaling;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.CutsForInk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemberStateTest
{
    private static MemberState state(int level, int xp, int hp, int unspent, String weapon, List<String> abilities)
    {
        return new MemberState("gunther", level, xp, hp, StatPoints.NONE, unspent, weapon, abilities);
    }

    @Test void aStateIsWhatItSays()
    {
        MemberState state = new MemberState("gunther", 3, 40, 150, new StatPoints(1, 2, 0, 0, 0), 4, "thesmosia", List.of("basic-attack"));

        assertEquals("gunther", state.id());
        assertEquals(3, state.level());
        assertEquals(40, state.xp());
        assertEquals(150, state.hp());
        assertEquals(new StatPoints(1, 2, 0, 0, 0), state.points());
        assertEquals(4, state.unspentPoints());
        assertEquals("thesmosia", state.weaponId());
        assertEquals(List.of("basic-attack"), state.abilityIds());
    }

    @Test void theStateOfAMemberIsWhatHeIsRightNow()
    {
        Gunther gunther = new Gunther(new Stats(184, 8, 4, 10, 3), 9, 3, ApRules.defaults(), InkRules.defaults());
        gunther.equip(new Weapon("thesmosia", "Thesmosia", WeaponType.SWORD, 32, Scaling.none()));
        gunther.setHp(100);

        MemberState state = MemberState.of(gunther);

        assertEquals("gunther", state.id());
        assertEquals(1, state.level());
        assertEquals(100, state.hp());
        assertEquals(new StatPoints(0, 8, 4, 10, 3), state.points());
        assertEquals("thesmosia", state.weaponId());
        assertEquals(List.of("basic-attack", "cuts-for-ink", "stasigraphic-fury", "thirst-for-ink"), state.abilityIds());
    }

    @Test void theListOfAbilitiesCannotBeChangedFromOutside()
    {
        java.util.ArrayList<String> abilities = new java.util.ArrayList<>(List.of("basic-attack"));
        MemberState state = state(1, 0, 10, 0, "thesmosia", abilities);

        abilities.add("cuts-for-ink");

        assertEquals(List.of("basic-attack"), state.abilityIds());
        assertThrows(UnsupportedOperationException.class, () -> state.abilityIds().add("x"));
    }

    @Test void badValuesAreRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> state(0, 0, 10, 0, "thesmosia", List.of()));
        assertThrows(IllegalArgumentException.class, () -> state(1, -1, 10, 0, "thesmosia", List.of()));
        assertThrows(IllegalArgumentException.class, () -> state(1, 0, -1, 0, "thesmosia", List.of()));
        assertThrows(IllegalArgumentException.class, () -> state(1, 0, 10, -1, "thesmosia", List.of()));
        assertThrows(IllegalArgumentException.class, () -> state(1, 0, 10, 0, "Thesmosia", List.of()));
        assertThrows(IllegalArgumentException.class, () -> state(1, 0, 10, 0, "thesmosia", List.of("Cuts for Ink")));
        assertThrows(IllegalArgumentException.class, () -> state(1, 0, 10, 0, "thesmosia", List.of("basic-attack", "basic-attack")));
        assertThrows(NullPointerException.class, () -> state(1, 0, 10, 0, null, List.of()));
    }
}
