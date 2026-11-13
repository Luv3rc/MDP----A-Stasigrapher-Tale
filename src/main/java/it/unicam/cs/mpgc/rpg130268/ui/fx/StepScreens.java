package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import it.unicam.cs.mpgc.rpg130268.game.EncounterSetup;
import it.unicam.cs.mpgc.rpg130268.game.StepOutcome;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.BattleStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.DefeatStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.DialogueStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.EndStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.RewardStep;
import it.unicam.cs.mpgc.rpg130268.game.StoryStep.TrekStep;
import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenterFactory;
import it.unicam.cs.mpgc.rpg130268.ui.StepHandlers;
import it.unicam.cs.mpgc.rpg130268.ui.reward.RewardPresenter;

import java.util.HashMap;
import java.util.Map;

final class StepScreens
{
    private StepScreens() { }

    static StepHandlers<GameScreen> screens(StepContext context)
    {
        return new StepHandlers<GameScreen>()
                .on(DialogueStep.class, step -> dialogue(step, context))
                .on(BattleStep.class, step -> battle(step, context))
                .on(RewardStep.class, step -> reward(step, context))
                .on(DefeatStep.class, step -> defeat(context))
                // These have no screen yet, so the player goes back to the main menu
                .on(TrekStep.class, step -> context.mainMenu())
                .on(EndStep.class, step -> context.mainMenu());
    }

    private static GameScreen dialogue(DialogueStep step, StepContext context)
    {
        return new DialogueScreen(context.flow().getProtagonist().getName(), step,
                () -> context.complete(new StepOutcome.Finished()));
    }

    private static GameScreen battle(BattleStep step, StepContext context)
    {
        EncounterSetup setup = step.encounter().spawn();
        context.setBattleArea(setup.area());

        return new BattleScreen(() -> BattlePresenterFactory.create(context.flow().getParty(), setup), new BattleExit()
        {
            @Override
            public void onWon(BattleEvent.BattleEnded end)
            {
                Map<String, Integer> xpByMember = new HashMap<>();
                end.xpByMember().forEach((member, xp) -> xpByMember.put(member.getId(), xp));

                context.complete(new StepOutcome.BattleWon(xpByMember));
            }

            @Override
            public void onLost() { context.complete(new StepOutcome.BattleLost()); }
        });
    }

    private static GameScreen reward(RewardStep step, StepContext context)
    {
        return new RewardScreen(new RewardPresenter(context.flow().getParty(), step.reward()), context.battleArea(),
                () -> context.complete(new StepOutcome.Finished()));
    }

    private static GameScreen defeat(StepContext context)
    {
        return new DefeatScreen(context.battleArea(),
                () -> context.complete(new StepOutcome.Retry()),
                () -> context.complete(new StepOutcome.GoBack()));
    }
}
