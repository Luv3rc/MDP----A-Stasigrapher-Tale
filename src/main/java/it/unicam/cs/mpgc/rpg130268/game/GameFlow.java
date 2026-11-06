package it.unicam.cs.mpgc.rpg130268.game;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.*;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;
import it.unicam.cs.mpgc.rpg130268.game.data.MemberCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/* *
    The game from the first dialogue to the end of the demo. Says what the player is doing now (current)
    and, when the UI tells it how that ended (complete), what comes next
    flow.current()                     // what to show (a StoryStep)
    flow.complete(outcome)             // the step is over. Then ask current() again

    The story goes on like this
    Dialogue --> Finished--> next step of the Story
    Battle   --> BattleWon  --> Reward --> Finished --> next step (or back to the trek, if it was met there)
             --> BattleLost --> Defeat --> Retry    --> the same Battle again, with the party as it was before it
                                       --> GoBack   --> the last safe moment
    Trek     --> Walked     --> still in the trek, or a Battle, or (when the walk is over) the next step

    GameFlow only decides where to go. The rest is done by smaller classes:
   * Party: the members, and making them again from their MemberStates (retry, load)
   * Trek: the walk and its dice
   * PendingBattle: the battle in progress and how the party was before it
    The same members go through all the steps, but after a retry or a load they are new objects, so the UI must
    always ask getParty() when a battle starts

    Loading a game from a file is save() and restore()
    A retry is different, since it goes back to the state kept in memory before the battle

    Safe points: every time the player is at a dialogue or in a trek the game remembers that moment
    GoBack returns to the last one, so after a defeat the player can leave the battle
    (in the tutorial, the start of the game; in a trek, the step before the ambush)
    Whoever is interested in these moments (the autosave) can listen with addSafePointListener
* */
public final class GameFlow
{
    private final Story story;
    private final Party party;
    private final RandomGenerator random;

    private int index;                      // where we are in the story
    private StoryStep current;
    private Trek trek;
    private PendingBattle battle;           // only from the start of a battle to the reward or the retry

    private GameSnapshot safePoint;         // the last moment at a dialogue or in a trek: where GoBack returns
    private final List<Consumer<GameSnapshot>> safePointListeners = new ArrayList<>();

    public GameFlow(Story story, MemberCatalog members, RandomGenerator random)
    {
        this.story = Objects.requireNonNull(story, "story must not be null");
        this.party = new Party(members);
        this.random = Objects.requireNonNull(random, "random must not be null");

        newGame();
    }

    public GameFlow(GameData data, RandomGenerator random)
    {
        this(data.story(), data.members(), random);
    }


    // ---------- Starting ----------

    public void newGame()
    {
        party.reset();
        battle = null;
        safePoint = null;
        enter(0, 0);

        if (safePoint == null)
            markSafePoint();
    }

    // Goes back to a saved game. The snapshot must come from a save() made with a story that has the same step ids
    public void restore(GameSnapshot snapshot)
    {
        Objects.requireNonNull(snapshot, "snapshot must not be null");

        int target = story.indexOf(snapshot.stepId());              // it throws if the step does not exist

        if (story.node(target).step() instanceof TrekStep trek && snapshot.trekProgress() >= trek.length())
            throw new IllegalArgumentException("this save is past the end of the trek");

        party.restore(snapshot.party());
        battle = null;
        safePoint = snapshot;           // a saved game is a safe moment (and nobody needs to be told again)
        enter(target, snapshot.trekProgress());
    }

    // The game as it is now, to write in a save file. Only when nothing is in progress (while reading a dialogue
    // or walking in a trek); empty during a battle and after it, until the reward and the defeat are closed
    public Optional<GameSnapshot> save()
    {
        if (current instanceof DialogueStep || current instanceof TrekStep)
            return Optional.of(snapshot());

        return Optional.empty();
    }

    public void addSafePointListener(Consumer<GameSnapshot> listener)
    {
        safePointListeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }


    // ---------- Looking at the game ----------

    public StoryStep current() { return current; }

    public List<PartyMember> getParty() { return party.members(); }

    public PartyMember getProtagonist() { return party.protagonist(); }

    public int getTrekProgress() { return trek == null ? 0 : trek.progress(); }

    public boolean isOver() { return current instanceof EndStep; }


    // ---------- Moving the story ----------

    // The current step is over
    public void complete(StepOutcome outcome)
    {
        Objects.requireNonNull(outcome, "outcome must not be null");

        switch (current)
        {
            case DialogueStep ignored ->
            {
                expect(outcome, StepOutcome.Finished.class);
                advance();
            }
            case BattleStep ignored -> battleFinished(outcome);
            case RewardStep ignored ->
            {
                expect(outcome, StepOutcome.Finished.class);
                afterReward();
            }
            case DefeatStep ignored -> defeatAnswered(outcome);
            case TrekStep ignored ->
            {
                expect(outcome, StepOutcome.Walked.class);
                walk();
            }
            case EndStep ignored -> throw new IllegalStateException("the demo is over");
        }
    }


    // ---------- Inside ----------

    private void battleFinished(StepOutcome outcome)
    {
        switch (outcome)
        {
            case StepOutcome.BattleWon won -> current = new RewardStep(battle.rewardFor(party.state(), won.xpByMember()));
            case StepOutcome.BattleLost ignored -> current = new DefeatStep();
            default -> throw new IllegalArgumentException("a battle ends with BattleWon or BattleLost, not " + outcome);
        }
    }

    private void defeatAnswered(StepOutcome outcome)
    {
        switch (outcome)
        {
            case StepOutcome.Retry ignored -> retry();
            case StepOutcome.GoBack ignored -> restore(safePoint);
            default -> throw new IllegalArgumentException("a defeat ends with Retry or GoBack, not " + outcome);
        }
    }

    // Repeat fight
    private void retry()
    {
        party.restore(battle.partyBefore());
        current = new BattleStep(battle.encounter());
    }

    private void afterReward()
    {
        boolean fromTrek = battle.fromTrek();
        battle = null;

        if (fromTrek)
        {
            current = story.node(index).step();     // the trek goes on, from where it was
            markSafePoint();
        }
        else
            advance();
    }

    private void walk()
    {
        switch (trek.walk())
        {
            case Trek.Result.Arrived ignored -> advance();
            case Trek.Result.Ambush ambush -> beginBattle(ambush.encounter(), true);
            case Trek.Result.Quiet ignored -> markSafePoint();    // nothing happens: the player is still in the trek
        }
    }

    private void advance() { enter(index + 1, 0); }

    private void enter(int stepIndex, int trekProgress)
    {
        index = stepIndex;
        trek = null;
        StoryStep step = story.node(stepIndex).step();

        if (step instanceof BattleStep battleStep)
        {
            beginBattle(battleStep.encounter(), false);
            return;
        }

        if (step instanceof TrekStep trekStep)
            trek = new Trek(trekStep, trekProgress, random);

        current = step;

        if (step instanceof DialogueStep || step instanceof TrekStep)
            markSafePoint();
    }

    private void markSafePoint()
    {
        GameSnapshot now = snapshot();

        if (now.equals(safePoint))
            return;

        safePoint = now;
        safePointListeners.forEach(listener -> listener.accept(now));
    }

    private GameSnapshot snapshot() { return new GameSnapshot(story.node(index).id(), getTrekProgress(), party.state()); }

    // This is where a retry will go back to
    private void beginBattle(Encounter encounter, boolean fromTrek)
    {
        battle = new PendingBattle(encounter, fromTrek, party.state());
        current = new BattleStep(encounter);
    }

    private void expect(StepOutcome outcome, Class<? extends StepOutcome> expected)
    {
        if (!expected.isInstance(outcome))
            throw new IllegalArgumentException(current.getClass().getSimpleName() + " ends with "
                    + expected.getSimpleName() + ", not " + outcome);
    }
}
