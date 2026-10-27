package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.ui.BattlePresenter;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

// A console has no animation, but a window can SHOW the timing: a bar where a marker (now) runs towards the
// impact, over the zones where a parry (green) or a dodge (blue) is good. The player presses when the
// marker is inside the zone. It only draws what the presenter's cue says
final class TimingBar extends Pane
{
    private static final double WIDTH = 560;
    private static final double HEIGHT = 30;
    private static final long BEFORE_MS = 900;    // how much time before the impact the bar shows
    private static final long AFTER_MS = 200;     // and after it

    private final Rectangle dodgeZone = new Rectangle(0, 0, 0, HEIGHT);
    private final Rectangle parryZone = new Rectangle(0, 0, 0, HEIGHT);
    private final Rectangle impactLine = new Rectangle(0, 0, 3, HEIGHT);
    private final Rectangle marker = new Rectangle(0, 0, 6, HEIGHT);

    TimingBar()
    {
        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);

        Rectangle track = new Rectangle(WIDTH, HEIGHT);
        track.setFill(Color.web("#2b2b3c"));

        dodgeZone.setFill(Color.web("#2980b9", 0.8));
        parryZone.setFill(Color.web("#27ae60", 0.9));
        impactLine.setFill(Color.WHITE);
        marker.setFill(Color.web("#e74c3c"));

        getChildren().addAll(track, dodgeZone, parryZone, impactLine, marker);
        setVisible(false);
    }

    void show(BattlePresenter.Cue cue)
    {
        double from = cue.impactMs() - BEFORE_MS;

        place(dodgeZone, cue.dodge(), cue.dodgeFromMs(), cue.dodgeToMs(), from);
        place(parryZone, cue.parry(), cue.parryFromMs(), cue.parryToMs(), from);

        impactLine.setX(x(cue.impactMs(), from) - impactLine.getWidth() / 2);
        marker.setX(x(cue.nowMs(), from) - marker.getWidth() / 2);

        setVisible(true);
    }

    void hide() { setVisible(false); }

    private void place(Rectangle zone, boolean used, long fromMs, long toMs, double barFromMs)
    {
        zone.setVisible(used);

        double left = x(fromMs, barFromMs);
        zone.setX(left);
        zone.setWidth(Math.max(0, x(toMs, barFromMs) - left));
    }

    // Where a moment is on the bar (never outside of it)
    private double x(double timeMs, double barFromMs)
    {
        double part = (timeMs - barFromMs) / (BEFORE_MS + AFTER_MS);
        return Math.max(0, Math.min(1, part)) * WIDTH;
    }
}