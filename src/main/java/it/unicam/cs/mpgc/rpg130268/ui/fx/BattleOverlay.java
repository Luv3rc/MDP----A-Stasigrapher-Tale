package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleEvent;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/* *
    The two texts that appear over the battle:
    - the announcement: the text of an ability that announces itself ("Mattias27 heals!"), in the top-center of the
      window. The engine makes the ability begin only after it is gone
    - the banner of the end of the battle (VICTORY / DEFEAT)
    The screen puts the two nodes where they have to be (the announcement over the whole window, the banner over the field)
* */
final class BattleOverlay
{
    private static final long ANNOUNCEMENT_FADE_IN_MS = 250;
    private static final long ANNOUNCEMENT_FADE_OUT_MS = 350;

    private final Label announcementLabel = new Label();
    private final Label bannerLabel = new Label();

    private SequentialTransition announcement = null;        // the one that is on the screen now, if any

    BattleOverlay()
    {
        announcementLabel.setStyle("-fx-text-fill: #f5ebd0; -fx-font-size: 16px; -fx-font-weight: bold; "
                + "-fx-background-color: rgba(10, 10, 14, 0.78); -fx-border-color: #d2b45a; -fx-border-width: 1.5; "
                + "-fx-padding: 4 20 4 20; -fx-effect: dropshadow(gaussian, black, 6, 0.4, 0, 1);");
        announcementLabel.setVisible(false);
        announcementLabel.setMouseTransparent(true);
        StackPane.setAlignment(announcementLabel, Pos.TOP_CENTER);
        StackPane.setMargin(announcementLabel, new Insets(12, 0, 0, 0));

        bannerLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 44px; -fx-font-weight: bold; "
                + "-fx-effect: dropshadow(gaussian, black, 8, 0.8, 0, 0);");
        bannerLabel.setVisible(false);
        bannerLabel.setMouseTransparent(true);
    }

    Node announcementNode() { return announcementLabel; }

    Node bannerNode() { return bannerLabel; }

    // The text fades in, stays, and fades out so that it is gone exactly when the engine starts the ability
    void announce(BattleEvent.Announced e)
    {
        if (announcement != null) announcement.stop();

        announcementLabel.setText(e.text());
        announcementLabel.setOpacity(0);
        announcementLabel.setVisible(true);

        FadeTransition in = new FadeTransition(Duration.millis(ANNOUNCEMENT_FADE_IN_MS), announcementLabel);
        in.setFromValue(0);
        in.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.millis(
                Math.max(0, e.durationMs() - ANNOUNCEMENT_FADE_IN_MS - ANNOUNCEMENT_FADE_OUT_MS)));

        FadeTransition out = new FadeTransition(Duration.millis(ANNOUNCEMENT_FADE_OUT_MS), announcementLabel);
        out.setFromValue(1);
        out.setToValue(0);

        announcement = new SequentialTransition(in, stay, out);
        announcement.setOnFinished(event -> announcementLabel.setVisible(false));
        announcement.play();
    }

    void showOutcome(BattleEvent.BattleEnded e)
    {
        String text = switch (e.outcome())
        {
            case VICTORY -> "VICTORY!   +" + e.xp() + " XP";
            case DEFEAT -> "DEFEAT";
            case ONGOING -> "";
        };

        bannerLabel.setText(text);
        bannerLabel.setVisible(true);
    }

    // A new battle: nothing of the old one stays on the screen
    void reset()
    {
        if (announcement != null) announcement.stop();
        announcementLabel.setVisible(false);
        bannerLabel.setVisible(false);
    }
}