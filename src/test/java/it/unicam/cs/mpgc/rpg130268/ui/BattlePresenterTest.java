package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleClock;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattlePhase;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleRules;
import it.unicam.cs.mpgc.rpg130268.combat.battle.FirstUsableAbility;
import it.unicam.cs.mpgc.rpg130268.combat.battle.LiveBattleContext;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageRules;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionType;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAttack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class BattlePresenterTest
{
    // Nobody ever crits (the roll is always 0.99)
    private static class NoCrits implements RandomGenerator
    {
        @Override public double nextDouble() { return 0.99; }
        @Override public long nextLong() { return 0; }
    }

    // The numbers: Gunther (50 HP, faster) hits a goblin for 18 with a basic attack, which takes 400 ms.
    // The goblin's swing takes 1500 ms, so after the strike it starts at 400 and lands at 1900.
    // A perfect parry gives a counter of 28
    private Gunther gunther()
    {
        Gunther gunther = new Gunther(new Stats(50, 10, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults());
        gunther.equip(new Weapon("Test sword", WeaponType.SWORD, 10, ScalingGrade.NONE));
        return gunther;
    }

    private Enemy goblin(int hp, int xp)
    {
        Enemy goblin = new Enemy("Goblin", new Stats(hp, 10, 2, 3, 0), 2, xp);
        goblin.equip(new Weapon("Club", WeaponType.UNARMED, 10, ScalingGrade.NONE));
        goblin.learn(e -> new EnemyAttack(e, "Club swing", TargetType.ONE_ENEMY,
                List.of(new Hit(1500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL))));
        return goblin;
    }

    private BattlePresenter presenter(Gunther gunther, List<Enemy> enemies)
    {
        LiveBattleContext live = new LiveBattleContext(new DamageCalculator(DamageRules.defaults(), new NoCrits()),
                ReactionWindows.defaults(), BattleRules.defaults(), new BattleClock());
        List<PartyMember> party = List.of(gunther);
        Battle battle = new Battle(party, enemies, live, new FirstUsableAbility());

        return new BattlePresenter(battle, party, enemies, ReactionWindows.defaults(), new InputLockout(500));
    }

    // Started, and the first events shown: the player can choose
    private BattlePresenter ready(Gunther gunther, List<Enemy> enemies)
    {
        BattlePresenter presenter = presenter(gunther, enemies);
        presenter.start();
        presenter.advance(0);
        return presenter;
    }

    private Ability basicAttack(BattlePresenter presenter)
    {
        return presenter.getAbilities().stream().filter(a -> a.getName().equals("Basic Attack")).findFirst().orElseThrow();
    }

    // ---------- Choosing ----------

    @Test void thePlayerCanChooseOnlyWhenTheFirstEventsHaveBeenShown()
    {
        BattlePresenter presenter = presenter(gunther(), List.of(goblin(100, 30)));
        presenter.start();

        assertFalse(presenter.isChoosing());                 // the turn has started but not been shown yet
        assertTrue(presenter.getAbilities().isEmpty());

        presenter.advance(0);

        assertTrue(presenter.isChoosing());
        assertTrue(presenter.getAbilities().stream().anyMatch(a -> a.getName().equals("Basic Attack")));
        assertTrue(presenter.getLog().contains("-- Turn of Gunther --"));
    }

    @Test void severalEnemiesAskForATargetAndTheChoiceCanBeCancelled()
    {
        Enemy first = goblin(100, 30);
        Enemy second = goblin(100, 50);
        BattlePresenter presenter = ready(gunther(), List.of(first, second));

        presenter.chooseAbility(basicAttack(presenter));

        assertTrue(presenter.isTargeting());
        assertEquals(List.of(first, second), presenter.getTargetCandidates());
        assertEquals(100, first.getHp());                    // nothing happened yet

        presenter.cancel();
        assertFalse(presenter.isTargeting());

        presenter.chooseAbility(basicAttack(presenter));
        presenter.chooseTarget(second);

        assertEquals(100, first.getHp());
        assertEquals(100 - 18, second.getHp());
        assertFalse(presenter.isChoosing());
    }

    @Test void theAreaIsTheDefaultOneUntilTheBattleIsPlacedSomewhere()
    {
        BattlePresenter presenter = ready(gunther(), List.of(goblin(100, 30)));
        assertEquals(BattlePresenter.DEFAULT_AREA, presenter.getArea());

        presenter.setArea("  forest ");
        assertEquals("forest", presenter.getArea());

        presenter.setArea("   ");                           // nothing: back to the default
        assertEquals(BattlePresenter.DEFAULT_AREA, presenter.getArea());

        presenter.setArea(null);
        assertEquals(BattlePresenter.DEFAULT_AREA, presenter.getArea());
    }

    @Test void aSingleTargetIsChosenAtOnceUnlessThePlayerMustConfirmIt()
    {
        Enemy only = goblin(100, 30);
        BattlePresenter presenter = ready(gunther(), List.of(only));

        presenter.setAlwaysAskTarget(true);
        presenter.chooseAbility(basicAttack(presenter));

        assertTrue(presenter.isTargeting());                 // asked, even if there is only one
        assertEquals(List.of(only), presenter.getTargetCandidates());
        assertEquals(100, only.getHp());                     // nothing happened until it is confirmed

        presenter.chooseTarget(only);

        assertEquals(100 - 18, only.getHp());
    }

    @Test void aTargetThatIsNotACandidateIsIgnored()
    {
        Gunther g = gunther();
        BattlePresenter presenter = ready(g, List.of(goblin(100, 30), goblin(100, 50)));
        presenter.chooseAbility(basicAttack(presenter));

        presenter.chooseTarget(g);

        assertTrue(presenter.isTargeting());
    }

    @Test void choosingWhileSomethingIsHappeningIsIgnored()
    {
        Enemy goblin = goblin(100, 30);
        BattlePresenter presenter = ready(gunther(), List.of(goblin));
        Ability basic = basicAttack(presenter);
        presenter.chooseAbility(basic);

        presenter.chooseAbility(basic);

        assertEquals(100 - 18, goblin.getHp());              // only once
    }

    // ---------- What the player sees follows the clock ----------

    @Test void theBarsChangeWhenTheEventsHappenNotWhenTheRulesResolve()
    {
        Enemy goblin = goblin(100, 30);
        BattlePresenter presenter = ready(gunther(), List.of(goblin));
        presenter.chooseAbility(basicAttack(presenter));

        assertEquals(100 - 18, goblin.getHp());              // the rules already did it
        assertEquals(100, presenter.getEnemies().get(0).getShownHp());   // but the strike has not landed on the screen

        presenter.advance(399);
        assertEquals(100, presenter.getEnemies().get(0).getShownHp());

        List<BattleEvent> shown = presenter.advance(1);

        assertEquals(100 - 18, presenter.getEnemies().get(0).getShownHp());
        assertTrue(shown.stream().anyMatch(e -> e instanceof BattleEvent.HitResolved));
        assertTrue(presenter.getLog().contains("Gunther -> Goblin: 18 damage"));
    }

    @Test void theBarsShowTheCounterWhenItLands()
    {
        BattlePresenter presenter = ready(gunther(), List.of(goblin(100, 30)));
        presenter.chooseAbility(basicAttack(presenter));
        presenter.advance(400);                              // the strike lands; the goblin's swing starts
        presenter.advance(1500);                             // 1900: the impact

        presenter.press(ReactionType.PARRY);
        presenter.advance(87);                               // 1987: judged

        assertEquals(50, presenter.getParty().get(0).getShownHp());
        assertEquals(100 - 18 - 28, presenter.getEnemies().get(0).getShownHp());
        assertTrue(presenter.getLog().stream().anyMatch(line -> line.contains("parried!")));
    }

    @Test void theApIsTheRealOneAndTheLevelsAreShown()
    {
        Gunther g = gunther();
        BattlePresenter presenter = ready(g, List.of(goblin(100, 30)));

        assertEquals(g.getAp().getCurrentAp(), presenter.getParty().get(0).getAp());
        assertTrue(presenter.getParty().get(0).isPartyMember());
        assertEquals(2, presenter.getEnemies().get(0).getLevel());
        assertEquals(0, presenter.getEnemies().get(0).getAp());
    }

    // ---------- The cue ----------

    @Test void theCueFollowsTheHitsOfTheEnemyAttack()
    {
        BattlePresenter presenter = ready(gunther(), List.of(goblin(100, 30)));
        presenter.chooseAbility(basicAttack(presenter));

        presenter.advance(100);
        assertTrue(presenter.getCue().isEmpty());            // the player's own strike: nothing to react to

        presenter.advance(300);                              // 400: the swing starts and lands at 1900
        BattlePresenter.Cue cue = presenter.getCue().orElseThrow();
        assertEquals(400, cue.startMs());
        assertEquals(1900, cue.impactMs());
        assertTrue(cue.parry());
        assertEquals(1750, cue.parryFromMs());               // 150 ms early
        assertEquals(1950, cue.parryToMs());                 // 50 ms late
        assertTrue(cue.dodge());
        assertEquals(1637, cue.dodgeFromMs());               // 263 ms early
        assertEquals(1987, cue.dodgeToMs());                 // 87 ms late

        presenter.advance(1600);                             // 2000: the windows are closed
        assertTrue(presenter.getCue().isEmpty());
    }

    // ---------- The lockout ----------

    @Test void aKeyThatCountsForNothingLocksTheKeysForAWhile()
    {
        BattlePresenter presenter = ready(gunther(), List.of(goblin(100, 30)));
        presenter.chooseAbility(basicAttack(presenter));
        presenter.advance(400);                              // the swing is on its way

        presenter.advance(100);                              // 500
        presenter.press(ReactionType.PARRY);                 // far too early
        assertTrue(presenter.isLocked());
        assertEquals(500, presenter.getLockedRemainingMs());

        presenter.advance(100);                              // 600
        presenter.press(ReactionType.PARRY);                 // locked: ignored, no second penalty
        assertEquals(1, presenter.getLog().stream().filter(line -> line.contains("Missed!")).count());

        presenter.advance(1250);                             // 1850: the lock ended at 1000 and the window is open
        assertFalse(presenter.isLocked());
        presenter.press(ReactionType.PARRY);
        presenter.advance(200);

        assertEquals(50, presenter.getParty().get(0).getShownHp());   // a perfect parry
    }

    @Test void keysDuringThePlayersOwnStrikeCostNothing()
    {
        BattlePresenter presenter = ready(gunther(), List.of(goblin(100, 30)));
        presenter.chooseAbility(basicAttack(presenter));

        presenter.press(ReactionType.PARRY);

        assertFalse(presenter.isLocked());
    }

    // ---------- The end ----------

    @Test void victoryIsShownWhenItsTimeComes()
    {
        BattlePresenter presenter = ready(gunther(), List.of(goblin(10, 30)));
        presenter.chooseAbility(basicAttack(presenter));

        presenter.advance(399);
        assertFalse(presenter.isOver());

        presenter.advance(1);

        assertTrue(presenter.isOver());
        assertEquals(30, presenter.getEnd().orElseThrow().xp());
        assertEquals(BattlePhase.OVER, presenter.getPhase());
        assertFalse(presenter.getEnemies().get(0).isAlive());
        assertTrue(presenter.getLog().contains("VICTORY! Every survivor gains 30 XP."));
    }

    // ---------- Construction ----------

    @Test void thePresenterNeedsAllItsParts()
    {
        Gunther g = gunther();
        List<Enemy> enemies = List.of(goblin(100, 30));
        Battle battle = new Battle(List.of(g), enemies, new BattleClock());

        assertThrows(NullPointerException.class, () -> new BattlePresenter(null, List.of(g), enemies, ReactionWindows.defaults(), new InputLockout(1)));
        assertThrows(NullPointerException.class, () -> new BattlePresenter(battle, null, enemies, ReactionWindows.defaults(), new InputLockout(1)));
        assertThrows(NullPointerException.class, () -> new BattlePresenter(battle, List.of(g), null, ReactionWindows.defaults(), new InputLockout(1)));
        assertThrows(NullPointerException.class, () -> new BattlePresenter(battle, List.of(g), enemies, null, new InputLockout(1)));
        assertThrows(NullPointerException.class, () -> new BattlePresenter(battle, List.of(g), enemies, ReactionWindows.defaults(), null));
    }

    @Test void theDemoBattleStartsAndWaitsForTheFastest()
    {
        BattlePresenter presenter = DemoBattles.create();

        presenter.start();
        presenter.advance(0);

        assertTrue(presenter.isChoosing());
        assertEquals(1, presenter.getParty().size());
        assertEquals(2, presenter.getEnemies().size());
    }
}