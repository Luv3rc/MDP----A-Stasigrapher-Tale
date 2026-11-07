package it.unicam.cs.mpgc.rpg130268.ui.menu;

import it.unicam.cs.mpgc.rpg130268.ui.question.Choice;
import it.unicam.cs.mpgc.rpg130268.ui.question.QuestionPrompt;
import it.unicam.cs.mpgc.rpg130268.ui.question.QuestionView;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/* *
    The logic of the main menu, with no JavaFX (like BattlePresenter for the battle). It knows what is on the screen,
    what is selected and what the keys and the clicks of the player do. The window only draws view() and tells the
    presenter what the player did. When the player asks for something the program has to do (start a game, load
    one, quit) the method gives back an Action, and otherwise it gives NONE.

    There are two screens. The TITLE shows the name of the game and "press a button", and the MENU has new game,
    load and quit. On top of the menu there can be a question.
    - QUIT asks "return to desktop?", after Quit
    - OVERWRITE says that the automatic save will be replaced. It comes after New Game, and only if there is a save
      to lose
    - a notice, when something went wrong and the player has to know (the saved game could not be loaded)
    Every question starts with Cancel selected, so a distracted Enter does nothing bad. Load can't be chosen if
    there is no save.
* */
public final class MainMenuPresenter
{
    public enum Screen { TITLE, MENU }

    public enum Item
    {
        NEW_GAME("New game"), LOAD("Load"), QUIT("Quit");

        private final String label;

        Item(String label) { this.label = label; }

        public String label() { return label; }
    }

    public enum Action { NONE, NEW_GAME, LOAD_GAME, QUIT }

    // What the player is being asked. The notice is not here because it asks for nothing
    private enum Question { QUIT, OVERWRITE }

    // What the window shows
    public record ItemView(Item item, String label, boolean enabled, boolean selected) { }

    public record View(Screen screen, List<ItemView> items, Optional<QuestionView> question) { }

    private final boolean hasSave;
    private final QuestionPrompt prompt = new QuestionPrompt();
    private Screen screen = Screen.TITLE;
    private Item selected = Item.NEW_GAME;
    private Question asked;                         // what the prompt is asking, null for a notice or when nothing is asked

    // hasSave says that there is a game to load. When there is, a New Game would overwrite it, so the player is asked first
    public MainMenuPresenter(boolean hasSave) { this.hasSave = hasSave; }

    // ---------- What to show ----------

    public View view()
    {
        List<ItemView> items = Arrays.stream(Item.values())
                .map(item -> new ItemView(item, item.label(), isEnabled(item), item == selected)).toList();

        return new View(screen, items, prompt.view());
    }

    // ---------- What the player does ----------

    // Any key or click on the title screen opens the menu, and it does nothing anywhere else
    public void pressAnyButton()
    {
        if (screen == Screen.TITLE)
        {
            screen = Screen.MENU;
            selected = Item.NEW_GAME;
        }
    }

    // Up and down, or left and right in a question. In the menu it goes to the next item that can be chosen,
    // round and round
    public void move(int steps)
    {
        if (screen == Screen.TITLE || steps == 0)
            return;

        if (prompt.isAsking())
        {
            prompt.toggle();
            return;
        }

        Item[] items = Item.values();
        int direction = steps > 0 ? 1 : -1;
        int position = selected.ordinal();

        for (int i = 0; i < items.length; i++)
        {
            position = Math.floorMod(position + direction, items.length);

            if (isEnabled(items[position]))
            {
                selected = items[position];
                return;
            }
        }
    }

    // The mouse over an item. It does nothing for a greyed item, during a question or on the title screen
    public void select(Item item)
    {
        if (screen == Screen.MENU && !prompt.isAsking() && isEnabled(item))
            selected = item;
    }

    // Enter, which is the selected item or the selected button of the question
    public Action activate()
    {
        if (screen == Screen.TITLE)
            return Action.NONE;

        return prompt.isAsking() ? choose(prompt.selected()) : press(selected);
    }

    // A click on an item, which also selects it
    public Action press(Item item)
    {
        if (screen == Screen.TITLE || prompt.isAsking() || !isEnabled(item))
            return Action.NONE;

        selected = item;

        return switch (item)
        {
            case NEW_GAME -> hasSave ? ask(Question.OVERWRITE, "Overwrite the automatic save?",
                    "Starting a new game will replace it.") : Action.NEW_GAME;
            case LOAD -> Action.LOAD_GAME;
            case QUIT -> ask(Question.QUIT, "Return to desktop?", "");
        };
    }

    // A click on Confirm or Cancel of the question (or on the OK of a notice)
    public Action choose(Choice answer)
    {
        if (!prompt.isAsking())
            return Action.NONE;

        Question answered = asked;
        asked = null;
        prompt.close();

        if (answer == Choice.CANCEL || answered == null)
            return Action.NONE;

        return answered == Question.QUIT ? Action.QUIT : Action.NEW_GAME;
    }

    // Esc closes the question, since nothing else uses it
    public void back()
    {
        if (prompt.isAsking())
            choose(Choice.CANCEL);
    }

    // Tells the player something that went wrong, on top of the menu (for example why the game could not be loaded).
    // He closes it with OK
    public void showNotice(String title, String message)
    {
        asked = null;
        prompt.notice(title, message);
    }

    // ---------- Inside ----------

    private boolean isEnabled(Item item) { return item != Item.LOAD || hasSave; }

    private Action ask(Question question, String title, String message)
    {
        asked = question;
        prompt.ask(title, message);
        return Action.NONE;
    }
}
