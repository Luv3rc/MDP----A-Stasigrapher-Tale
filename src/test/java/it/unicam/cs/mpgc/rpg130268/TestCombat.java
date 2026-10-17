package it.unicam.cs.mpgc.rpg130268;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.LevelRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApPool;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkPool;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.gunther.StasigraphicFury;
import it.unicam.cs.mpgc.rpg130268.combat.skills.gunther.ThirstForInk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatsTest {
    @Test void validStatsAreCreated() {
        Stats s = new Stats(50, 8, 4, 6, 3);
        assertEquals(50, s.maxHealth());
    }
    @Test void zeroMaxHealthThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Stats(0, 1, 1, 1, 1));
    }
    @Test void negativeStatThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Stats(10, -1, 1, 1, 1));
    }
}

class ApPoolTest {
    @Test void gainIsCappedAtMax() {
        ApPool p = new ApPool(9, 8);
        p.gainAp(5);
        assertEquals(9, p.getCurrentAp());
    }
    @Test void spendReducesAp() {
        ApPool p = new ApPool(9, 5);
        p.spendAp(3);
        assertEquals(2, p.getCurrentAp());
    }
    @Test void overspendingThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ApPool(9, 2).spendAp(3));
    }
    @Test void fillApReachesMax() {
        ApPool p = new ApPool(9, 0);
        p.fillAp();
        assertEquals(9, p.getCurrentAp());
    }
    @Test void resetSetsValue() {
        ApPool p = new ApPool(9, 7);
        p.resetToStartingValue();
        assertEquals(7, p.getCurrentAp());
    }
}

class InkPoolTest {
    private InkPool pool(int current) { return new InkPool(8, current, InkRules.defaults()); }

    @Test void gainIsCappedAtMax() {
        InkPool p = pool(7);
        p.gainInk(5);
        assertEquals(8, p.getCurrentInk());
    }
    @Test void drainReturnsAmountAndEmptiesGauge() {
        InkPool p = pool(6);
        assertEquals(6, p.drainInk());
        assertEquals(0, p.getCurrentInk());
    }
    @Test void resetEmptiesGauge() {
        InkPool p = pool(5);
        p.resetInk();
        assertEquals(0, p.getCurrentInk());
    }
    @Test void overspendingThrows() {
        assertThrows(IllegalArgumentException.class, () -> pool(2).spendInk(3));
    }
}

class PartyMemberTest {
    private PartyMember member(int startingAp) {
        return new PartyMember("Test", new Stats(50, 5, 3, 4, 1), 9, startingAp,
                ApRules.defaults(), LevelRules.defaults(), 1, 0) {
            @Override protected void onLevelUp() { }
            @Override public List<Ability> getAbilities() { return List.of(); }
        };
    }

    @Test void turnStartGivesOneAp() {
        PartyMember m = member(2);
        m.onTurnStart();
        assertEquals(3, m.getAp().getCurrentAp());
    }
    @Test void turnStartApIsCapped() {
        PartyMember m = member(9);
        m.onTurnStart();
        assertEquals(9, m.getAp().getCurrentAp());
    }
    @Test void battleStartRestoresStartingAp() {
        PartyMember m = member(2);
        m.getAp().spendAp(2);
        m.onBattleStart();
        assertEquals(2, m.getAp().getCurrentAp());
    }
    @Test void gainXpLevelsUp() {
        PartyMember m = member(2);
        m.gainXp(250);                       // 200 needed for level 2
        assertEquals(2, m.getLevel());
        assertEquals(50, m.getCurrentXp());
    }
    @Test void gainXpCanLevelUpTwice() {
        PartyMember m = member(2);
        m.gainXp(500);                       // 200 + 300
        assertEquals(3, m.getLevel());
        assertEquals(0, m.getCurrentXp());
    }
    @Test void negativeXpThrows() {
        assertThrows(IllegalArgumentException.class, () -> member(2).gainXp(-1));
    }
}


class GuntherTest {
    private Gunther gunther() {
        return new Gunther(new Stats(50, 8, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults());
    }

    @Test void basicAttackGivesApAndInk() {
        Gunther g = gunther();
        g.onBasicAttackLanded();
        assertEquals(4, g.getAp().getCurrentAp());
        assertEquals(1, g.getInk().getCurrentInk());
    }
    @Test void battleStartResetsApAndInk() {
        Gunther g = gunther();
        g.getInk().fillInk();
        g.getAp().spendAp(2);
        g.onBattleStart();
        assertEquals(3, g.getAp().getCurrentAp());
        assertEquals(0, g.getInk().getCurrentInk());
    }
}

class FighterTest {
    private Fighter fighter() { return new Enemy("Goblin", new Stats(20, 5, 2, 4, 1)); }

    @Test void startsAtFullHp() { assertEquals(20, fighter().getHp()); }
    @Test void hpNeverGoesBelowZero() {
        Fighter f = fighter();
        f.takeDamage(999);
        assertEquals(0, f.getHp());
        assertFalse(f.isAlive());
    }
    @Test void healNeverExceedsMax() {
        Fighter f = fighter();
        f.takeDamage(5);
        f.heal(999);
        assertEquals(20, f.getHp());
    }
    @Test void negativeDamageThrows() {
        assertThrows(IllegalArgumentException.class, () -> fighter().takeDamage(-1));
    }
}


class AbilityTest
{
    private Gunther gunther(int startingAp)
    {
        return new Gunther(new Stats(50, 8, 4, 6, 3), 9, startingAp,
                ApRules.defaults(), InkRules.defaults());
    }

    private Enemy dummy() { return new Enemy("Dummy", new Stats(100, 1, 1, 1, 1)); }

    // ---------- Thirst for Ink ----------

    @Test
    void thirstForInkWorksWithExactlyOneAp()
    {
        Gunther g = gunther(1);
        new ThirstForInk(g).perform(g);

        assertEquals(1, g.getHp());
        assertEquals(9, g.getAp().getCurrentAp());   // paid 1, then refilled to max
        assertEquals(8, g.getInk().getCurrentInk()); // gauge full
    }

    @Test
    void thirstForInkNeedsOneAp()
    {
        assertFalse(new ThirstForInk(gunther(0)).canPerform());
    }

    @Test
    void thirstForInkOnAnotherTargetThrowsAndCostsNothing()
    {
        Gunther g = gunther(3);
        Enemy target = dummy();

        assertThrows(IllegalArgumentException.class, () -> new ThirstForInk(g).perform(target));
        assertEquals(3, g.getAp().getCurrentAp());
        assertEquals(50, g.getHp());
    }

    // ---------- Stasigraphic Fury ----------

    @Test
    void furyNeedEnoughInk()
    {
        Gunther g = gunther(4);
        g.getInk().gainInk(3);                       // 3 < 4
        assertFalse(new StasigraphicFury(g).canPerform());
    }

    @Test
    void furyNeedEnoughAp()
    {
        Gunther g = gunther(3);                      // 3 < 4
        g.getInk().gainInk(8);
        assertFalse(new StasigraphicFury(g).canPerform());
    }

    @Test
    void furyWorkWithExactApAndInk()
    {
        Gunther g = gunther(4);
        g.getInk().gainInk(4);
        Enemy target = dummy();

        new StasigraphicFury(g).perform(target);

        assertEquals(0, g.getAp().getCurrentAp());   // paid exactly 4
        assertEquals(0, g.getInk().getCurrentInk()); // gauge drained
        assertEquals(100 - 33, target.getHp());      // 4 hits * 6 agility * 1.4 = 33.6 -> 33
    }

    @Test
    void furyDealMoreDamageWithMoreInk()
    {
        Gunther low = gunther(4);
        low.getInk().gainInk(4);
        Enemy lowTarget = dummy();
        new StasigraphicFury(low).perform(lowTarget);

        Gunther high = gunther(4);
        high.getInk().gainInk(8);
        Enemy highTarget = dummy();
        new StasigraphicFury(high).perform(highTarget);

        assertTrue(highTarget.getHp() < lowTarget.getHp());
    }

    @Test
    void refusedFuryCostNothing()
    {
        Gunther g = gunther(4);
        g.getInk().gainInk(2);                       // not enough ink
        Enemy target = dummy();

        assertThrows(IllegalStateException.class, () -> new StasigraphicFury(g).perform(target));
        assertEquals(4, g.getAp().getCurrentAp());   // AP untouched
        assertEquals(2, g.getInk().getCurrentInk()); // ink untouched
        assertEquals(100, target.getHp());
    }

    @Test
    void furyOnSelfThrowAndCostNothing()
    {
        Gunther g = gunther(4);
        g.getInk().gainInk(4);

        assertThrows(IllegalArgumentException.class, () -> new StasigraphicFury(g).perform(g));
        assertEquals(4, g.getAp().getCurrentAp());
        assertEquals(4, g.getInk().getCurrentInk());
        assertEquals(50, g.getHp());
    }
}


class GuntherAbilitiesTest
{
    private Gunther gunther(int startingAp)
    {
        return new Gunther(new Stats(50, 8, 4, 6, 3), 9, startingAp,
                ApRules.defaults(), InkRules.defaults());
    }

    private long countByName(Gunther g, String name)
    {
        return g.getAbilities().stream().filter(a -> a.getName().equals(name)).count();
    }

    @Test
    void learnedAbilityAppearsInList()
    {
        Gunther g = gunther(4);
        g.learn(ThirstForInk::new);
        assertEquals(1, countByName(g, "Thirst for Ink"));
    }

    @Test
    void learningTheSameAbilityTwiceDoesNotDuplicateIt()
    {
        Gunther g = gunther(4);
        g.learn(StasigraphicFury::new);
        int before = g.getAbilities().size();
        g.learn(StasigraphicFury::new);
        assertEquals(before, g.getAbilities().size(), "La dimensione della lista delle abilità non dovrebbe aumentare imparando un duplicato");
    }

    @Test
    void learnedAbilityIsBoundToGunther()
    {
        Gunther g = gunther(4);
        g.learn(StasigraphicFury::new);
        g.getInk().gainInk(4);

        Ability cuts = g.getAbilities().stream()
                .filter(a -> a.getName().equals("Stasigraphic Fury"))
                .findFirst().orElseThrow();

        assertTrue(cuts.canPerform());               // his AP and his ink are used
    }

    @Test
    void abilityListCannotBeModifiedFromOutside()
    {
        Gunther g = gunther(4);
        List<Ability> list = g.getAbilities();
        assertThrows(UnsupportedOperationException.class, () -> list.clear());
    }
}