package it.unicam.cs.mpgc.rpg130268.combat.battle;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
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

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

class BattleTest
{
    // Nobody ever crits (the roll is always 0.99)
    private static class NoCrits implements RandomGenerator
    {
        @Override public double nextDouble() { return 0.99; }
        @Override public long nextLong() { return 0; }
    }

    private final BattleClock clock = new BattleClock();

    private LiveBattleContext live()
    {
        return new LiveBattleContext(new DamageCalculator(DamageRules.defaults(), new NoCrits()),
                ReactionWindows.defaults(), BattleRules.defaults(), clock);
    }

    // The numbers: Gunther (strength 10, defense 4, agility 6) hits a goblin (defense 2) for 18 with a normal hit.
    // The goblin (strength 10, agility 3) hits Gunther for 17. Gunther is faster, so he acts first.
    // A basic attack takes 400 ms; the goblin's swing lands 500 ms after it starts
    private Gunther gunther()
    {
        Gunther gunther = new Gunther(new Stats(50, 10, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults());
        gunther.equip(new Weapon("Test sword", WeaponType.SWORD, 10, ScalingGrade.NONE));
        return gunther;
    }

    private static Hit swing() { return new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL); }

    private Enemy goblinWithoutAbilities(int hp, int xp)
    {
        Enemy goblin = new Enemy("Goblin", new Stats(hp, 10, 2, 3, 0), 2, xp);
        goblin.equip(new Weapon("Club", WeaponType.UNARMED, 10, ScalingGrade.NONE));
        return goblin;
    }

    private Enemy goblin(int hp, int xp)
    {
        Enemy goblin = goblinWithoutAbilities(hp, xp);
        goblin.learn(e -> new EnemyAttack(e, "Club swing", TargetType.ONE_ENEMY, List.of(swing())));
        return goblin;
    }

    private Battle battle(List<PartyMember> party, List<Enemy> enemies) { return new Battle(party, enemies, live(), new FirstUsableAbility()); }

    private Battle battle(Gunther gunther, Enemy enemy) { return battle(List.of(gunther), List.of(enemy)); }

    private Ability basicAttack(Gunther gunther)
    {
        return gunther.getListOfAbility().stream().filter(a -> a.getName().equals("Basic Attack")).findFirst().orElseThrow();
    }

    // The player always uses a basic attack on the first enemy alive and never reacts. Returns how many frames it took
    private int playUntilOver(Battle battle, Gunther gunther)
    {
        int frames = 0;

        while (battle.getPhase() != BattlePhase.OVER && frames < 20_000)
        {
            if (battle.getPhase() == BattlePhase.WAITING_FOR_PLAYER)
                battle.perform(basicAttack(gunther), List.of(battle.enemiesOf(gunther).getFirst()));

            battle.update(10);
            frames++;
        }

        return frames;
    }

    // ---------- Starting ----------

    @Test void nothingWorksBeforeTheStart()
    {
        Gunther g = gunther();
        Battle battle = battle(g, goblin(100, 30));

        assertEquals(BattlePhase.NOT_STARTED, battle.getPhase());
        assertTrue(battle.getCurrentActor().isEmpty());
        assertThrows(IllegalStateException.class, () -> battle.update(10));
        assertThrows(IllegalStateException.class, () -> battle.perform(basicAttack(g), List.of(goblin(1, 0))));
    }

    @Test void startingGetsEverybodyReadyAndWaitsForTheFastest()
    {
        Gunther g = gunther();
        g.getAp().gainAp(5);                             // left over from before
        Battle battle = battle(g, goblin(100, 30));

        battle.start();

        assertEquals(BattlePhase.WAITING_FOR_PLAYER, battle.getPhase());
        assertSame(g, battle.getCurrentActor().orElseThrow());
        assertEquals(3 + ApRules.defaults().apGainOnTurnStart(), g.getAp().getCurrentAp());   // reset, then its turn began
        assertEquals(List.of(new BattleEvent.TurnStarted(g, 0)), battle.drainEvents());
    }

    @Test void aBattleStartsOnlyOnce()
    {
        Battle battle = battle(gunther(), goblin(100, 30));
        battle.start();

        assertThrows(IllegalStateException.class, battle::start);
    }

    // ---------- The turn of the player ----------

    @Test void thePlayerChoosesAndTheActionIsResolvedAtOnce()
    {
        Gunther g = gunther();
        Enemy goblin = goblin(100, 30);
        Battle battle = battle(g, goblin);
        battle.start();

        battle.perform(basicAttack(g), List.of(goblin));

        assertEquals(BattlePhase.PLAYING, battle.getPhase());
        assertEquals(100 - 18, goblin.getHp());          // the damage is done; its replay takes 400 ms
        assertSame(g, battle.getCurrentActor().orElseThrow());
    }

    @Test void theNextTurnWaitsUntilTheActionHasBeenPlayed()
    {
        Gunther g = gunther();
        Enemy goblin = goblin(100, 30);
        Battle battle = battle(g, goblin);
        battle.start();
        battle.perform(basicAttack(g), List.of(goblin));
        battle.drainEvents();

        battle.update(399);
        assertSame(g, battle.getCurrentActor().orElseThrow());           // the strike has not landed yet
        assertTrue(battle.drainEvents().stream().noneMatch(e -> e instanceof BattleEvent.TurnStarted));

        battle.update(1);

        assertSame(goblin, battle.getCurrentActor().orElseThrow());      // now it is the goblin's turn
        assertEquals(BattlePhase.PLAYING, battle.getPhase());            // and it attacked at once
        assertTrue(battle.drainEvents().stream().anyMatch(e -> e instanceof BattleEvent.AttackStarted));
    }

    @Test void aChoiceThatIsNotValidChangesNothingAndTheTurnKeepsWaiting()
    {
        Gunther g = gunther();
        Battle battle = battle(g, goblin(100, 30));
        battle.start();
        int ap = g.getAp().getCurrentAp();

        assertThrows(IllegalArgumentException.class, () -> battle.perform(basicAttack(g), List.of(g)));   // an enemy is needed
        assertThrows(IllegalArgumentException.class, () -> battle.perform(basicAttack(gunther()), List.of(goblin(1, 0))));   // not his
        assertThrows(NullPointerException.class, () -> battle.perform(null, List.of(g)));

        assertEquals(BattlePhase.WAITING_FOR_PLAYER, battle.getPhase());
        assertEquals(ap, g.getAp().getCurrentAp());
    }

    @Test void thePlayerCannotChooseWhileSomethingIsBeingPlayed()
    {
        Gunther g = gunther();
        Enemy goblin = goblin(100, 30);
        Battle battle = battle(g, goblin);
        battle.start();
        battle.perform(basicAttack(g), List.of(goblin));

        assertThrows(IllegalStateException.class, () -> battle.perform(basicAttack(g), List.of(goblin)));
    }

    // ---------- The turn of an enemy, on the clock ----------

    @Test void anEnemyAttackIsPlayedOnTheClockAndThePlayerParriesIt()
    {
        Gunther g = gunther();
        Enemy goblin = goblin(100, 30);
        Battle battle = battle(g, goblin);
        battle.start();
        battle.perform(basicAttack(g), List.of(goblin));
        battle.update(400);                              // the goblin's swing starts now (400), lands at 900

        assertTrue(battle.getRound() == 1);
        battle.update(500);
        assertTrue(battle.react(ReactionType.PARRY));    // on the impact
        battle.update(74);                               // 974: the reaction window is still open (75ms late window)
        assertEquals(BattlePhase.PLAYING, battle.getPhase());
        battle.update(1);                                // 975: the swing is judged and the round ends

        assertEquals(50, g.getHp());                     // parried
        assertEquals(100 - 18 - 28, goblin.getHp());     // and the counter hit it
        assertEquals(BattlePhase.WAITING_FOR_PLAYER, battle.getPhase());
        assertSame(g, battle.getCurrentActor().orElseThrow());
        assertEquals(2, battle.getRound());              // a new round: the player acts again
    }

    @Test void withoutAKeyTheEnemyAttackLands()
    {
        Gunther g = gunther();
        Enemy goblin = goblin(100, 30);
        Battle battle = battle(g, goblin);
        battle.start();
        battle.perform(basicAttack(g), List.of(goblin));

        battle.update(400 + 500 + 87);

        assertEquals(50 - 17, g.getHp());
        assertEquals(BattlePhase.WAITING_FOR_PLAYER, battle.getPhase());
    }

    @Test void aKeyWhileWaitingForTheChoiceCountsForNothing()
    {
        Battle battle = battle(gunther(), goblin(100, 30));
        battle.start();

        assertFalse(battle.react(ReactionType.PARRY));
    }

    @Test void anEnemyWithNothingToDoPasses()
    {
        Gunther g = gunther();
        Enemy goblin = goblinWithoutAbilities(100, 30);
        Battle battle = battle(g, goblin);
        battle.start();
        battle.perform(basicAttack(g), List.of(goblin));
        battle.drainEvents();

        battle.update(400);

        assertEquals(BattlePhase.WAITING_FOR_PLAYER, battle.getPhase());   // straight back to the player
        assertEquals(2, battle.getRound());
        assertEquals(List.of(new BattleEvent.TurnStarted(goblin, 400), new BattleEvent.TurnStarted(g, 400)),
                battle.drainEvents().stream().filter(e -> e instanceof BattleEvent.TurnStarted).toList());
    }

    @Test void anAttackOnTheWholePartyIsOneAttackAndOneKeyCoversEveryone()
    {
        Gunther first = gunther();
        Gunther second = gunther();
        Enemy goblin = goblinWithoutAbilities(200, 30);
        goblin.learn(e -> new EnemyAttack(e, "Roar", TargetType.ALL_ENEMIES, List.of(swing())));
        Battle battle = battle(List.of(first, second), List.of(goblin));
        battle.start();

        battle.perform(basicAttack(first), List.of(goblin));
        battle.update(400);
        battle.perform(basicAttack(second), List.of(goblin));
        battle.update(400);                              // the roar starts at 800 and lands at 1300

        battle.update(500);
        assertTrue(battle.react(ReactionType.PARRY));
        battle.update(87);

        assertEquals(50, first.getHp());
        assertEquals(50, second.getHp());
        assertEquals(200 - 18 - 18 - 28 - 28, goblin.getHp());           // both counters landed
    }

    // ---------- The end ----------

    @Test void victoryGivesTheXpToThePartyThatIsStillStanding()
    {
        Gunther g = gunther();
        Gunther fallen = gunther();
        fallen.setHp(0);
        Enemy goblin = goblin(10, 30);
        Battle battle = battle(List.of(g, fallen), List.of(goblin));
        battle.start();
        battle.perform(basicAttack(g), List.of(goblin));
        battle.drainEvents();

        battle.update(400);                              // the strike has been played: the battle is over

        assertEquals(BattlePhase.OVER, battle.getPhase());
        assertEquals(BattleOutcome.VICTORY, battle.getOutcome());
        assertEquals(30, g.getCurrentXp());
        assertEquals(0, fallen.getCurrentXp());
        assertTrue(battle.getCurrentActor().isEmpty());
        assertEquals(List.of(new BattleEvent.BattleEnded(BattleOutcome.VICTORY, 30, 400)), battle.drainEvents());
    }

    @Test void theRewardIsTheSumOfAllTheEnemies()
    {
        Gunther g = gunther();
        Battle battle = battle(List.of(g), List.of(goblin(10, 30), goblin(10, 50)));
        battle.start();

        playUntilOver(battle, g);

        assertEquals(BattleOutcome.VICTORY, battle.getOutcome());
        assertEquals(80, g.getCurrentXp());
    }

    @Test void defeatGivesNoXp()
    {
        Gunther fragile = gunther();
        fragile.setHp(10);                               // the goblin's swing (17) kills
        Battle battle = battle(fragile, goblin(100, 30));
        battle.start();

        playUntilOver(battle, fragile);

        assertEquals(BattlePhase.OVER, battle.getPhase());
        assertEquals(BattleOutcome.DEFEAT, battle.getOutcome());
        assertEquals(0, fragile.getCurrentXp());
        assertTrue(battle.drainEvents().stream().anyMatch(e -> e instanceof BattleEvent.BattleEnded ended
                && ended.outcome() == BattleOutcome.DEFEAT && ended.xp() == 0));
    }

    @Test void aWholeBattleFromStartToXp()
    {
        Gunther g = gunther();
        Enemy goblin = goblin(40, 30);
        Battle battle = battle(g, goblin);
        battle.start();

        int frames = playUntilOver(battle, g);

        assertTrue(frames < 20_000);
        assertEquals(BattleOutcome.VICTORY, battle.getOutcome());
        assertEquals(3, battle.getRound());              // 3 basic attacks (18 each) to beat 40 HP
        assertEquals(50 - 17 - 17, g.getHp());           // and the goblin swung twice
        assertEquals(30, g.getCurrentXp());

        List<BattleEvent> events = battle.drainEvents();
        assertInstanceOf(BattleEvent.BattleEnded.class, events.getLast());
        assertEquals(1, events.stream().filter(e -> e instanceof BattleEvent.BattleEnded).count());
    }

    @Test void nothingHappensAfterTheEnd()
    {
        Gunther g = gunther();
        Enemy goblin = goblin(10, 30);
        Battle battle = battle(g, goblin);
        battle.start();
        battle.perform(basicAttack(g), List.of(goblin));
        battle.update(400);
        battle.drainEvents();

        battle.update(5000);

        assertEquals(BattlePhase.OVER, battle.getPhase());
        assertTrue(battle.drainEvents().isEmpty());
        assertEquals(30, g.getCurrentXp());              // the XP is given only once
    }

    // ---------- Who is who ----------

    @Test void alliesAndEnemiesAreTheLivingOnesFromThePointOfViewOfTheFighter()
    {
        Gunther first = gunther();
        Gunther fallen = gunther();
        fallen.setHp(0);
        Enemy goblin = goblin(100, 30);
        Battle battle = battle(List.of(first, fallen), List.of(goblin));

        assertEquals(List.of(first), battle.alliesOf(first));
        assertEquals(List.of(goblin), battle.enemiesOf(first));
        assertEquals(List.of(goblin), battle.alliesOf(goblin));
        assertEquals(List.of(first), battle.enemiesOf(goblin));
        assertThrows(IllegalArgumentException.class, () -> battle.alliesOf(gunther()));
        assertThrows(NullPointerException.class, () -> battle.enemiesOf(null));
    }

    // ---------- Construction ----------

    @Test void aBattleNeedsAllItsParts()
    {
        List<PartyMember> party = List.of(gunther());
        List<Enemy> enemies = List.of(goblin(100, 30));

        assertThrows(NullPointerException.class, () -> new Battle(null, enemies, live(), new FirstUsableAbility()));
        assertThrows(NullPointerException.class, () -> new Battle(party, null, live(), new FirstUsableAbility()));
        assertThrows(NullPointerException.class, () -> new Battle(party, enemies, null, new FirstUsableAbility()));
        assertThrows(NullPointerException.class, () -> new Battle(party, enemies, live(), null));
        assertThrows(IllegalArgumentException.class, () -> new Battle(List.of(), enemies, live(), new FirstUsableAbility()));
        assertThrows(IllegalArgumentException.class, () -> new Battle(party, List.of(), live(), new FirstUsableAbility()));
    }

    @Test void theBattleKeepsItsOwnCopyOfTheSides()
    {
        List<PartyMember> party = new ArrayList<>(List.of(gunther()));
        Battle battle = battle(party, List.of(goblin(100, 30)));

        party.clear();
        battle.start();

        assertEquals(BattlePhase.WAITING_FOR_PLAYER, battle.getPhase());
    }

    @Test void theDefaultBattleWorks()
    {
        Gunther g = gunther();
        Battle battle = new Battle(List.of(g), List.of(goblin(100, 30)), new BattleClock());

        battle.start();

        assertEquals(BattlePhase.WAITING_FOR_PLAYER, battle.getPhase());
    }

    // ---------- Time goes on in pieces ----------

    @Test void aBigStepGivesTheSameResultAsManySmallOnes()
    {
        // Two identical battles: one gets the time in a single step, the other 1 ms at a time
        Gunther bigG = gunther();
        Enemy bigGoblin = goblin(100, 30);
        BattleClock bigClock = new BattleClock();
        Battle big = new Battle(List.of(bigG), List.of(bigGoblin),
                new LiveBattleContext(new DamageCalculator(DamageRules.defaults(), new NoCrits()), ReactionWindows.defaults(), BattleRules.defaults(), bigClock),
                new FirstUsableAbility());

        Gunther smallG = gunther();
        Enemy smallGoblin = goblin(100, 30);
        Battle small = battle(smallG, smallGoblin);

        for (Battle b : List.of(big, small))
        {
            b.start();
            b.perform(basicAttack(b == big ? bigG : smallG), List.of(b == big ? bigGoblin : smallGoblin));
        }

        big.update(987);
        for (int i = 0; i < 987; i++) small.update(1);

        assertEquals(smallG.getHp(), bigG.getHp());
        assertEquals(smallGoblin.getHp(), bigGoblin.getHp());
        assertEquals(small.getPhase(), big.getPhase());
        assertEquals(small.getRound(), big.getRound());
        assertEquals(small.drainEvents().stream().map(BattleEvent::atMs).toList(),
                big.drainEvents().stream().map(BattleEvent::atMs).toList());
    }

    @Test void theNextActionStartsWhenThePreviousOneEndedNotWhenTheFrameEnded()
    {
        Gunther g = gunther();
        Enemy goblin = goblin(100, 30);
        Battle battle = battle(g, goblin);
        battle.start();
        battle.perform(basicAttack(g), List.of(goblin));
        battle.drainEvents();

        battle.update(450);                              // a slow frame: the player's strike ended at 400

        BattleEvent started = battle.drainEvents().stream().filter(e -> e instanceof BattleEvent.AttackStarted).findFirst().orElseThrow();
        assertEquals(400, started.atMs());
    }

    @Test void timeCannotGoBack()
    {
        Battle battle = battle(gunther(), goblin(100, 30));
        battle.start();

        assertThrows(IllegalArgumentException.class, () -> battle.update(-1));
    }
}