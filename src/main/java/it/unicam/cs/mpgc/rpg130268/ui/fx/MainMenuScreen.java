package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.menu.MainMenuPresenter;
import it.unicam.cs.mpgc.rpg130268.ui.menu.MainMenuPresenter.Action;
import it.unicam.cs.mpgc.rpg130268.ui.menu.MainMenuPresenter.Item;
import it.unicam.cs.mpgc.rpg130268.ui.menu.MainMenuPresenter.ItemView;
import it.unicam.cs.mpgc.rpg130268.ui.menu.MainMenuPresenter.Screen;
import it.unicam.cs.mpgc.rpg130268.ui.menu.MainMenuPresenter.View;
import it.unicam.cs.mpgc.rpg130268.ui.question.Choice;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/* *
    Who could've guessed, it's the main menu. It draws what the MainMenuPresenter says and tells it what the player
    does, and it has no logic of its own. The presenter gives back an Action when the program has to do something
    (new game,load, quit), and this screen passes it on with the three Runnables

    First there is the title of the game on black (src/main/resources/ui/main_menu.png) with "PRESS A BUTTON"
    under it. After any key or click come New game, Load and Quit, in the gold buttons of the battle (RpgButtons)
    TODO: Settings should be here too, but it will be done later--eventually
    The questions ("Return to desktop?", "Overwrite the automatic save?" and the notice that a game could not be
    loaded) are shown by the QuestionPopup

    Keys are W and S for up and down, A and D for Confirm and Cancel in a question, Enter or Space to choose and
    Esc to close the question. The mouse does the same, since going over a button selects it and a click chooses it.
    Without the picture of the title the name of the game is written in gold, and the game works anyway
* */
final class MainMenuScreen implements GameScreen
{
    private static final String LOGO = "/ui/main_menu.png";
    private static final double LOGO_WIDTH = 900;
    private static final double LOGO_TOP = -80;              // so it starts above the window, and the buttons fit under the text
    private static final double PRESS_TOP = 430;
    private static final double MENU_TOP = 425;
    private static final double MENU_GAP = 14;

    private final MainMenuPresenter presenter;
    private final Runnable onNewGame;
    private final Runnable onLoad;
    private final Runnable onQuit;

    private final Pane root = new Pane();
    private final Label pressLabel = new Label("PRESS A BUTTON");
    private final VBox menuBox = new VBox(MENU_GAP);
    private final Map<Item, Button> buttons = new EnumMap<>(Item.class);
    private final QuestionPopup popup;
    private final FadeTransition pulse = new FadeTransition(Duration.seconds(1.1), pressLabel);

    MainMenuScreen(MainMenuPresenter presenter, Runnable onNewGame, Runnable onLoad, Runnable onQuit)
    {
        this.presenter = Objects.requireNonNull(presenter, "presenter must not be null");
        this.onNewGame = Objects.requireNonNull(onNewGame, "onNewGame must not be null");
        this.onLoad = Objects.requireNonNull(onLoad, "onLoad must not be null");
        this.onQuit = Objects.requireNonNull(onQuit, "onQuit must not be null");

        root.setStyle("-fx-background-color: black;");
        RpgButtons rpgButtons = new RpgButtons(GameStyle.apply(root));

        Region logo = makeLogo();
        logo.setMouseTransparent(true);

        pressLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px;");
        pressLabel.setPrefWidth(SceneHost.DESIGN_WIDTH);
        pressLabel.setAlignment(Pos.CENTER);
        pressLabel.setLayoutY(PRESS_TOP);
        pressLabel.setMouseTransparent(true);
        pulse.setFromValue(0.25);
        pulse.setToValue(1);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);

        for (Item item : Item.values())
        {
            Button button = rpgButtons.menu(item.label().toUpperCase());
            button.setOnMouseEntered(event -> { presenter.select(item); render(); });
            button.setOnAction(event -> { handle(presenter.press(item)); render(); });
            buttons.put(item, button);
            menuBox.getChildren().add(button);
        }

        menuBox.setAlignment(Pos.TOP_CENTER);
        menuBox.setPrefWidth(SceneHost.DESIGN_WIDTH);
        menuBox.setLayoutY(MENU_TOP);
        menuBox.setPickOnBounds(false);

        popup = new QuestionPopup(rpgButtons,
                () -> { handle(presenter.choose(Choice.CONFIRM)); render(); },
                () -> { handle(presenter.choose(Choice.CANCEL)); render(); });
        popup.getRoot().setPrefSize(SceneHost.DESIGN_WIDTH, SceneHost.DESIGN_HEIGHT);

        root.getChildren().addAll(logo, pressLabel, menuBox, popup.getRoot());

        // On the title screen a click anywhere counts as a button
        root.setOnMouseClicked(event ->
        {
            if (presenter.view().screen() == Screen.TITLE)
            {
                presenter.pressAnyButton();
                render();
            }
        });

        render();
    }

    @Override
    public Region getRoot() { return root; }

    @Override
    public void onShow() { pulse.play(); }

    @Override
    public void stop() { pulse.stop(); }

    @Override
    public void onKeyPressed(KeyEvent event)
    {
        View view = presenter.view();

        if (view.screen() == Screen.TITLE)
            presenter.pressAnyButton();
        else if (view.question().isPresent())
            switch (event.getCode())
            {
                case A -> presenter.move(-1);
                case D -> presenter.move(1);
                case ENTER, SPACE -> handle(presenter.activate());
                case ESCAPE -> presenter.back();
                default -> { }
            }
        else
            switch (event.getCode())
            {
                case W -> presenter.move(-1);
                case S -> presenter.move(1);
                case ENTER, SPACE -> handle(presenter.activate());
                default -> { }
            }

        event.consume();
        render();
    }


    // ---------- Inside ----------

    // Everything is drawn again from what the presenter says. It is little work and it is the simplest way to never
    // show something that is not true anymore
    private void render()
    {
        View view = presenter.view();
        boolean onTitle = view.screen() == Screen.TITLE;

        pressLabel.setVisible(onTitle);
        menuBox.setVisible(!onTitle);

        for (ItemView item : view.items())
        {
            Button button = buttons.get(item.item());
            button.setDisable(!item.enabled());
            GameStyle.setSelected(button, item.selected() && !onTitle);
        }

        view.question().ifPresentOrElse(popup::show, popup::hide);
    }

    private void handle(Action action)
    {
        switch (action)
        {
            case NEW_GAME -> onNewGame.run();
            case LOAD_GAME -> onLoad.run();
            case QUIT -> onQuit.run();
            case NONE -> { }
        }
    }

    // The picture of the title, or its name in gold if the picture is not there
    private static Region makeLogo()
    {
        Pane holder = new Pane();

        try (InputStream in = MainMenuScreen.class.getResourceAsStream(LOGO))
        {
            if (in != null)
            {
                ImageView picture = new ImageView(new Image(in));
                picture.setPreserveRatio(true);
                picture.setSmooth(true);
                picture.setFitWidth(LOGO_WIDTH);
                picture.setLayoutX((SceneHost.DESIGN_WIDTH - LOGO_WIDTH) / 2);
                picture.setLayoutY(LOGO_TOP);
                holder.getChildren().add(picture);
                return holder;
            }
        }
        catch (java.io.IOException ignored) { }

        Label name = new Label("TALE OF A STASIGRAPHER");
        name.setStyle("-fx-text-fill: #d2b45a; -fx-font-size: 54px; "
                + "-fx-font-family: 'Palatino Linotype', 'Book Antiqua', Georgia, serif;");
        name.setPrefWidth(SceneHost.DESIGN_WIDTH);
        name.setAlignment(Pos.CENTER);
        name.setLayoutY(180);
        holder.getChildren().add(name);

        return holder;
    }
}
