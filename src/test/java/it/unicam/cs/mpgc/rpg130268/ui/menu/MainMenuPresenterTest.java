package it.unicam.cs.mpgc.rpg130268.ui.menu;

import it.unicam.cs.mpgc.rpg130268.ui.menu.MainMenuPresenter.*;
import it.unicam.cs.mpgc.rpg130268.ui.question.Choice;
import it.unicam.cs.mpgc.rpg130268.ui.question.QuestionView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainMenuPresenterTest
{
    private static MainMenuPresenter menu(boolean hasSave)
    {
        MainMenuPresenter menu = new MainMenuPresenter(hasSave);
        menu.pressAnyButton();
        return menu;
    }

    private static Item selected(MainMenuPresenter menu)
    {
        return menu.view().items().stream().filter(ItemView::selected).findFirst().orElseThrow().item();
    }

    private static ItemView item(MainMenuPresenter menu, Item item)
    {
        return menu.view().items().stream().filter(view -> view.item() == item).findFirst().orElseThrow();
    }

    // ---------- Title ----------

    @Test void theGameStartsOnTheTitleScreen()
    {
        assertEquals(Screen.TITLE, new MainMenuPresenter(true).view().screen());
    }

    @Test void onTheTitleScreenOnlyAnyButtonDoesSomething()
    {
        MainMenuPresenter menu = new MainMenuPresenter(true);

        menu.move(1);
        menu.select(Item.QUIT);
        assertEquals(Action.NONE, menu.activate());
        assertEquals(Action.NONE, menu.press(Item.QUIT));
        assertEquals(Screen.TITLE, menu.view().screen());

        menu.pressAnyButton();
        assertEquals(Screen.MENU, menu.view().screen());
    }

    @Test void theMenuOpensWithNewGameSelected()
    {
        assertEquals(Item.NEW_GAME, selected(menu(true)));
    }

    @Test void theMenuShowsTheThreeItemsInOrder()
    {
        assertEquals(java.util.List.of("New game", "Load", "Quit"),
                menu(true).view().items().stream().map(ItemView::label).toList());
    }

    // ---------- Moving ----------

    @Test void movingGoesThroughTheItemsRoundAndRound()
    {
        MainMenuPresenter menu = menu(true);

        menu.move(1);
        assertEquals(Item.LOAD, selected(menu));
        menu.move(1);
        assertEquals(Item.QUIT, selected(menu));
        menu.move(1);
        assertEquals(Item.NEW_GAME, selected(menu));
        menu.move(-1);
        assertEquals(Item.QUIT, selected(menu));
    }

    @Test void withoutASaveLoadIsGreyedAndMovingSkipsIt()
    {
        MainMenuPresenter menu = menu(false);

        assertFalse(item(menu, Item.LOAD).enabled());
        assertTrue(item(menu, Item.NEW_GAME).enabled());

        menu.move(1);
        assertEquals(Item.QUIT, selected(menu));
        menu.move(-1);
        assertEquals(Item.NEW_GAME, selected(menu));
    }

    @Test void theMouseSelectsAnItemButNotAGreyedOne()
    {
        MainMenuPresenter menu = menu(false);

        menu.select(Item.QUIT);
        assertEquals(Item.QUIT, selected(menu));

        menu.select(Item.LOAD);
        assertEquals(Item.QUIT, selected(menu));
    }

    @Test void loadCantBeChosenWithoutASave()
    {
        assertEquals(Action.NONE, menu(false).press(Item.LOAD));
    }

    // ---------- New game ----------

    @Test void newGameWithoutASaveStartsAtOnce()
    {
        assertEquals(Action.NEW_GAME, menu(false).activate());
    }

    @Test void newGameWithASaveAsksBeforeOverwritingIt()
    {
        MainMenuPresenter menu = menu(true);

        assertEquals(Action.NONE, menu.activate());

        QuestionView question = menu.view().question().orElseThrow();
        assertEquals("Overwrite the automatic save?", question.title());
        assertFalse(question.message().isBlank());
    }

    @Test void theQuestionStartsOnCancelSoAnAbsentMindedEnterDoesNothing()
    {
        MainMenuPresenter menu = menu(true);
        menu.activate();

        assertEquals(Choice.CANCEL, menu.view().question().orElseThrow().selected());
        assertEquals(Action.NONE, menu.activate());
        assertTrue(menu.view().question().isEmpty());
    }

    @Test void confirmingTheOverwriteStartsTheNewGame()
    {
        MainMenuPresenter menu = menu(true);
        menu.activate();
        menu.move(1);

        assertEquals(Choice.CONFIRM, menu.view().question().orElseThrow().selected());
        assertEquals(Action.NEW_GAME, menu.activate());
    }

    @Test void clickingConfirmOrCancelWorksToo()
    {
        MainMenuPresenter menu = menu(true);
        menu.press(Item.NEW_GAME);
        assertEquals(Action.NEW_GAME, menu.choose(Choice.CONFIRM));

        menu.press(Item.NEW_GAME);
        assertEquals(Action.NONE, menu.choose(Choice.CANCEL));
        assertTrue(menu.view().question().isEmpty());
    }

    // ---------- Load ----------

    @Test void loadWithASaveLoadsIt()
    {
        MainMenuPresenter menu = menu(true);
        menu.move(1);

        assertEquals(Action.LOAD_GAME, menu.activate());
    }

    // ---------- Quit ----------

    @Test void quitAsksBeforeClosing()
    {
        MainMenuPresenter menu = menu(true);

        assertEquals(Action.NONE, menu.press(Item.QUIT));
        assertEquals("Return to desktop?", menu.view().question().orElseThrow().title());
    }

    @Test void confirmingQuitClosesTheGameAndCancellingGoesBackToTheMenu()
    {
        MainMenuPresenter menu = menu(true);
        menu.press(Item.QUIT);
        assertEquals(Action.QUIT, menu.choose(Choice.CONFIRM));

        menu.press(Item.QUIT);
        assertEquals(Action.NONE, menu.choose(Choice.CANCEL));
        assertEquals(Item.QUIT, selected(menu));
    }

    // ---------- During a question ----------

    @Test void escapeClosesTheQuestion()
    {
        MainMenuPresenter menu = menu(true);
        menu.press(Item.QUIT);

        menu.back();

        assertTrue(menu.view().question().isEmpty());
    }

    @Test void whileAskingTheItemsCantBeUsed()
    {
        MainMenuPresenter menu = menu(true);
        menu.press(Item.QUIT);

        assertEquals(Action.NONE, menu.press(Item.NEW_GAME));
        menu.select(Item.LOAD);
        assertEquals(Item.QUIT, selected(menu));
    }

    @Test void movingInAQuestionSwitchesBetweenConfirmAndCancel()
    {
        MainMenuPresenter menu = menu(true);
        menu.press(Item.QUIT);

        menu.move(1);
        assertEquals(Choice.CONFIRM, menu.view().question().orElseThrow().selected());
        menu.move(-1);
        assertEquals(Choice.CANCEL, menu.view().question().orElseThrow().selected());
    }

    @Test void aNewQuestionAlwaysStartsOnCancel()
    {
        MainMenuPresenter menu = menu(true);
        menu.press(Item.QUIT);
        menu.move(1);
        menu.back();

        menu.press(Item.QUIT);

        assertEquals(Choice.CANCEL, menu.view().question().orElseThrow().selected());
    }

    // ---------- A notice ----------

    @Test void aNoticeIsShownWithOnlyAnOkAndTheMenuCantBeUsedMeanwhile()
    {
        MainMenuPresenter menu = menu(true);

        menu.showNotice("The saved game can't be loaded", "It was made by another version.");

        QuestionView notice = menu.view().question().orElseThrow();
        assertEquals("The saved game can't be loaded", notice.title());
        assertEquals("It was made by another version.", notice.message());
        assertEquals("OK", notice.confirmLabel());
        assertFalse(notice.hasCancel());
        assertEquals(Choice.CONFIRM, notice.selected());
        assertEquals(Action.NONE, menu.press(Item.NEW_GAME));
    }

    @Test void aNoticeDoesNothingWhenItIsClosed()
    {
        MainMenuPresenter menu = menu(true);
        menu.showNotice("Problem", "Something went wrong.");

        assertEquals(Action.NONE, menu.activate());

        assertTrue(menu.view().question().isEmpty());
        assertEquals(Item.NEW_GAME, selected(menu));
    }

    @Test void aNoticeCanBeClosedWithEscapeOrByClickingOk()
    {
        MainMenuPresenter menu = menu(true);

        menu.showNotice("Problem", "Something went wrong.");
        menu.back();
        assertTrue(menu.view().question().isEmpty());

        menu.showNotice("Problem", "Something went wrong.");
        assertEquals(Action.NONE, menu.choose(Choice.CONFIRM));
        assertTrue(menu.view().question().isEmpty());
    }

    @Test void movingInANoticeChangesNothingBecauseThereIsOnlyOneButton()
    {
        MainMenuPresenter menu = menu(true);
        menu.showNotice("Problem", "Something went wrong.");

        menu.move(1);

        assertEquals(Choice.CONFIRM, menu.view().question().orElseThrow().selected());
    }

    @Test void aNoticeNeverStartsTheGameEvenIfAnOverwriteWasBeingAsked()
    {
        MainMenuPresenter menu = menu(true);
        menu.press(Item.NEW_GAME);

        menu.showNotice("Problem", "Something went wrong.");

        assertEquals(Action.NONE, menu.choose(Choice.CONFIRM));
    }
}
