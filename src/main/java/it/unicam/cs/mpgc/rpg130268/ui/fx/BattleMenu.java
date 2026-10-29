package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.BasicAttack;
import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenter;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/* *
    The menu next to Gunther: ATTACK / ABILITIES / ITEMS. ABILITIES opens a second page (two columns of three).
    When the battle is over it shows the question "Retry battle?" instead.

    The menu is built again only when what it depends on changes (commandMode), so a button is never replaced while
    the mouse is on it. It does not decide anything: it asks the presenter, and tells the screen what to do
    when the player wants to retry (or to quit) with the two Runnables.
* */
final class BattleMenu
{
    // The keys of the menu, in order: the first entry is F, the second E, the third W...
    private static final List<KeyCode> KEYS = List.of(KeyCode.F, KeyCode.E, KeyCode.W, KeyCode.R, KeyCode.G, KeyCode.V);

    private static final String PAGE_MAIN = "main";
    private static final String PAGE_ABILITIES = "abilities";

    private static final double ABILITY_GAP = 6;

    private final VBox box = new VBox(10);
    private final RpgButtons buttons;
    private final Supplier<BattlePresenter> current;      // the battle of now (it is a new one at every retry)
    private final Runnable onRetry;
    private final Runnable onQuit;

    private String commandMode = "";
    private String page = PAGE_MAIN;

    BattleMenu(RpgButtons buttons, Supplier<BattlePresenter> current, Runnable onRetry, Runnable onQuit)
    {
        this.buttons = Objects.requireNonNull(buttons, "buttons must not be null");
        this.current = Objects.requireNonNull(current, "current must not be null");
        this.onRetry = Objects.requireNonNull(onRetry, "onRetry must not be null");
        this.onQuit = Objects.requireNonNull(onQuit, "onQuit must not be null");

        box.setAlignment(Pos.BOTTOM_LEFT);
    }

    VBox getRoot() { return box; }

    // True while the second page (the abilities) is open: the screen writes the keys that work now
    boolean isOnAbilitiesPage() { return page.equals(PAGE_ABILITIES); }

    // A new battle: the main page, and the menu is built again on the next frame
    void reset()
    {
        commandMode = "";
        page = PAGE_MAIN;
    }

    // Called every frame: the menu is built again only when it changes
    void update()
    {
        // Every turn starts from the main menu
        if (!current.get().isChoosing()) page = PAGE_MAIN;

        String mode = commandMode();

        if (!mode.equals(commandMode))
        {
            commandMode = mode;
            build();
        }
    }

    // The keys of the menu (F, E, W... and Esc). The screen calls it only while the player is choosing
    void onKey(KeyCode code)
    {
        if (code == KeyCode.ESCAPE)
            page = PAGE_MAIN;
        else if (KEYS.contains(code))
            pick(KEYS.indexOf(code) + 1);
    }

    // ---------- Inside ----------

    // The n-th entry of the menu (1 = first, written with the key KEYS.get(n - 1))
    private void pick(int number)
    {
        BattlePresenter presenter = current.get();

        if (page.equals(PAGE_MAIN))
        {
            Ability basic = basicAttack();

            if (number == 1 && basic != null && basic.canPerform())
                presenter.chooseAbility(basic);
            else if (number == 2 && !otherAbilities().isEmpty())
                page = PAGE_ABILITIES;
        }
        else
        {
            List<Ability> abilities = otherAbilities();
            if (number <= abilities.size() && abilities.get(number - 1).canPerform())
                presenter.chooseAbility(abilities.get(number - 1));
        }
    }

    // The plain strike is the ATTACK of the menu; everything else is under ABILITIES
    private Ability basicAttack()
    {
        return current.get().getAbilities().stream().filter(a -> a instanceof BasicAttack).findFirst().orElse(null);
    }

    private List<Ability> otherAbilities()
    {
        return current.get().getAbilities().stream().filter(a -> !(a instanceof BasicAttack)).toList();
    }

    // What the menu depends on: if it is the same as before, it is the same
    private String commandMode()
    {
        BattlePresenter presenter = current.get();

        if (presenter.isOver())
            return "over";

        if (presenter.isTargeting())
            return "target";

        if (presenter.isChoosing())
            return "choose:" + page + ":" + presenter.getAbilities().stream().map(a -> a.getName() + a.canPerform()).collect(Collectors.joining(","));

        return "play";
    }

    // There is no menu while the target is chosen: it is chosen on the battlefield (A / D, Enter)
    private void build()
    {
        BattlePresenter presenter = current.get();

        box.getChildren().clear();

        if (presenter.isOver())
            buildRetryMenu();
        else if (presenter.isTargeting())
            return;
        else if (presenter.isChoosing())
        {
            if (page.equals(PAGE_ABILITIES))
                buildAbilitiesMenu();
            else
                buildMainMenu();
        }
    }

    private void buildRetryMenu()
    {
        box.getChildren().add(prompt("Retry battle?"));

        Button yes = buttons.menu("Yes   (Y)");
        yes.setOnAction(event -> onRetry.run());

        Button no = buttons.menu("No   (N)");
        no.setOnAction(event -> onQuit.run());

        box.getChildren().addAll(yes, no);
    }

    // ATTACK / ABILITIES / ITEMS
    private void buildMainMenu()
    {
        Ability basic = basicAttack();

        Button attack = buttons.menu("Attack   (F)");
        attack.setDisable(basic == null || !basic.canPerform());
        attack.setOnAction(event -> current.get().chooseAbility(basic));

        Button abilities = buttons.menu("Abilities   (E)");
        abilities.setDisable(otherAbilities().isEmpty());
        abilities.setOnAction(event -> page = PAGE_ABILITIES);

        // There are no items in the game yet
        Button items = buttons.menu("Items   (W)");
        items.setDisable(true);
        items.setTooltip(new Tooltip("No items yet"));

        box.getChildren().addAll(attack, abilities, items);
    }

    // The abilities in two columns of three: 1-3 on the left, 4-6 on the right
    private void buildAbilitiesMenu()
    {
        GridPane grid = new GridPane();
        grid.setHgap(ABILITY_GAP);
        grid.setVgap(ABILITY_GAP);

        List<Ability> abilities = otherAbilities();
        for (int i = 0; i < abilities.size(); i++)
        {
            Ability ability = abilities.get(i);
            Button button = buttons.ability(KEYS.get(i).getName(), ability, abilities.size() > 3);
            button.setDisable(!ability.canPerform());
            button.setOnAction(event -> current.get().chooseAbility(ability));
            grid.add(button, i / 3, i % 3);
        }

        box.getChildren().add(grid);
    }

    private Label prompt(String text)
    {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #ccccdd; -fx-font-size: 14px; -fx-effect: dropshadow(gaussian, black, 4, 0.8, 0, 0);");
        label.setWrapText(true);
        return label;
    }
}