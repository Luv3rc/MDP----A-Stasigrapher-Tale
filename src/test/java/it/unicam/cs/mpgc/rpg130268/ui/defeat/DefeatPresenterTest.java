package it.unicam.cs.mpgc.rpg130268.ui.defeat;

import it.unicam.cs.mpgc.rpg130268.ui.defeat.DefeatPresenter.Action;
import it.unicam.cs.mpgc.rpg130268.ui.defeat.DefeatPresenter.View;
import it.unicam.cs.mpgc.rpg130268.ui.question.Choice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefeatPresenterTest
{
    // A presenter with everything already shown, so the question is there
    private static DefeatPresenter asking()
    {
        DefeatPresenter presenter = new DefeatPresenter();
        presenter.tick(DefeatPresenter.QUESTION_AT_MS);

        return presenter;
    }

    @Test void atTheStartThePictureIsAsItWasAndNothingElseIsThere()
    {
        View view = new DefeatPresenter().view();

        assertEquals("Thesmos won't ever be freed", view.phrase());
        assertEquals(0.0, view.darkness());
        assertEquals(0.0, view.phraseOpacity());
        assertTrue(view.question().isEmpty());
    }

    @Test void theScreenGetsDarkerAndDarkerUntilItIsBlack()
    {
        DefeatPresenter presenter = new DefeatPresenter();

        presenter.tick(DefeatPresenter.DARKEN_MS / 4);
        double quarter = presenter.view().darkness();
        presenter.tick(DefeatPresenter.DARKEN_MS / 4);
        double half = presenter.view().darkness();
        presenter.tick(DefeatPresenter.DARKEN_MS / 2);

        assertEquals(0.25, quarter);
        assertEquals(0.5, half);
        assertEquals(1.0, presenter.view().darkness());
    }

    @Test void theScreenStaysBlackAfterwards()
    {
        DefeatPresenter presenter = new DefeatPresenter();

        presenter.tick(DefeatPresenter.QUESTION_AT_MS + 5000);

        assertEquals(1.0, presenter.view().darkness());
    }

    @Test void thePhraseDoesNotShowWhileTheScreenIsStillGettingDark()
    {
        DefeatPresenter presenter = new DefeatPresenter();

        presenter.tick(DefeatPresenter.DARKEN_MS - 1);

        assertTrue(presenter.view().darkness() < 1.0);
        assertEquals(0.0, presenter.view().phraseOpacity());

        presenter.tick(1);

        assertEquals(1.0, presenter.view().darkness());
        assertEquals(0.0, presenter.view().phraseOpacity());        // it starts to come in only now
    }

    @Test void thePhraseFadesInOnceTheScreenIsBlackAndThenItStays()
    {
        DefeatPresenter presenter = new DefeatPresenter();
        presenter.tick(DefeatPresenter.DARKEN_MS);

        presenter.tick(DefeatPresenter.FADE_IN_MS / 2);
        assertEquals(0.5, presenter.view().phraseOpacity());

        presenter.tick(DefeatPresenter.FADE_IN_MS / 2);
        assertEquals(1.0, presenter.view().phraseOpacity());

        presenter.tick(DefeatPresenter.READ_MS);
        assertEquals(1.0, presenter.view().phraseOpacity());         // it does not go away when the question comes
    }

    @Test void theQuestionComesOnlyAfterThePhraseIsCompleteAndHasBeenReadForAMoment()
    {
        DefeatPresenter presenter = new DefeatPresenter();

        presenter.tick(DefeatPresenter.DARKEN_MS + DefeatPresenter.FADE_IN_MS);
        assertEquals(1.0, presenter.view().phraseOpacity());
        assertTrue(presenter.view().question().isEmpty());

        presenter.tick(DefeatPresenter.READ_MS - 1);
        assertFalse(presenter.isQuestionTime());
        assertTrue(presenter.view().question().isEmpty());

        presenter.tick(1);
        assertTrue(presenter.isQuestionTime());
        assertTrue(presenter.view().question().isPresent());
    }

    @Test void beforeTheQuestionNothingThePlayerPressesCounts()
    {
        DefeatPresenter presenter = new DefeatPresenter();
        presenter.tick(DefeatPresenter.QUESTION_AT_MS - 1);

        assertEquals(Action.NONE, presenter.activate());
        assertEquals(Action.NONE, presenter.choose(Choice.CONFIRM));
        assertEquals(Action.NONE, presenter.choose(Choice.CANCEL));
    }

    @Test void theQuestionHasYesAndNoAndNoIsSelected()
    {
        View view = asking().view();

        assertEquals("Do you want to retry the battle?", view.question().orElseThrow().title());
        assertEquals("Yes", view.question().orElseThrow().confirmLabel());
        assertEquals("No", view.question().orElseThrow().cancelLabel());
        assertEquals(Choice.CANCEL, view.question().orElseThrow().selected());
    }

    @Test void yesRetriesTheBattleAndNoGoesBack()
    {
        assertEquals(Action.RETRY, asking().choose(Choice.CONFIRM));
        assertEquals(Action.GO_BACK, asking().choose(Choice.CANCEL));
    }

    @Test void enterChoosesTheSelectedButtonAndMovingChangesIt()
    {
        DefeatPresenter presenter = asking();
        assertEquals(Choice.CANCEL, presenter.view().question().orElseThrow().selected());

        presenter.move();
        assertEquals(Choice.CONFIRM, presenter.view().question().orElseThrow().selected());
        assertEquals(Action.RETRY, presenter.activate());

        assertEquals(Action.GO_BACK, asking().activate());          // the first one selected is No
    }

    @Test void theAnswerCountsOnlyOnce()
    {
        DefeatPresenter presenter = asking();

        assertEquals(Action.RETRY, presenter.choose(Choice.CONFIRM));
        assertEquals(Action.NONE, presenter.choose(Choice.CONFIRM));
        assertEquals(Action.NONE, presenter.activate());
        assertTrue(presenter.view().question().isEmpty());

        presenter.tick(5000);
        assertTrue(presenter.view().question().isEmpty());          // it does not come back
    }

    @Test void theQuestionDoesNotStartAgainWhenTimeGoesOn()
    {
        DefeatPresenter presenter = asking();
        presenter.move();

        presenter.tick(1000);

        assertEquals(Choice.CONFIRM, presenter.view().question().orElseThrow().selected());     // the selection was kept
    }

    @Test void timeCannotGoBack()
    {
        assertThrows(IllegalArgumentException.class, () -> new DefeatPresenter().tick(-1));
    }
}
