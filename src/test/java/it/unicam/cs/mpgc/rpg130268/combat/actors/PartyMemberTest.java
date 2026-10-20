package it.unicam.cs.mpgc.rpg130268.combat.actors;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.LevelRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartyMemberTest {
    private PartyMember member(int startingAp) {
        return new PartyMember("Test", new Stats(50, 5, 3, 4, 1),
                Set.of(WeaponType.SWORD), 9, startingAp, ApRules.defaults(), LevelRules.defaults(), 1, 0)
            {
            @Override
            protected void onLevelUp() {
            }

            @Override
            public List<Ability> getListOfAbility() {
                return List.of();
            }
        };
    }

    @Test
    void turnStartGivesOneAp() {
        PartyMember m = member(2);
        m.onTurnStart();
        assertEquals(3, m.getAp().getCurrentAp());
    }

    @Test
    void turnStartApIsCapped() {
        PartyMember m = member(9);
        m.onTurnStart();
        assertEquals(9, m.getAp().getCurrentAp());
    }

    @Test
    void battleStartRestoresStartingAp() {
        PartyMember m = member(2);
        m.getAp().spendAp(2);
        m.onBattleStart();
        assertEquals(2, m.getAp().getCurrentAp());
    }

    @Test
    void gainXpLevelsUp() {
        PartyMember m = member(2);
        m.gainXp(250);                       // 200 needed for level 2
        assertEquals(2, m.getLevel());
        assertEquals(50, m.getCurrentXp());
    }

    @Test
    void gainXpCanLevelUpTwice() {
        PartyMember m = member(2);
        m.gainXp(500);                       // 200 + 300
        assertEquals(3, m.getLevel());
        assertEquals(0, m.getCurrentXp());
    }

    @Test
    void negativeXpThrows() {
        assertThrows(IllegalArgumentException.class, () -> member(2).gainXp(-1));
    }
}
