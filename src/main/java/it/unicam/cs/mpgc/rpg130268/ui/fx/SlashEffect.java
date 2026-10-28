package it.unicam.cs.mpgc.rpg130268.ui.fx;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/* *
    The "cut" of an attack: a thin triangle that comes from the side of the screen and goes through the target.
    It is only a view: it does not decide anything, it just draws what the battle says, ON THE BATTLE CLOCK.

    - An enemy attack: when the UI hears AttackStarted, a strike is scheduled for every hit and every target.
      The blade is where the clock says it is, so it is on the target exactly at the impact of the hit
      (the same moment the timing bar says), whatever the frame rate is
    - When the hit is judged (HitResolved) the strike learns how it ended:
        HIT      the blade goes on through the target, and leaves a scar that fades
        PARRIED  the blade stops, bounces back and a ring of sparks opens on the target
        DODGED   the blade goes through a faded target (the target should step aside, see FighterCard)
      Between the impact and the verdict (at most the late window of the reaction) the blade waits on the target
    - The blade always starts on the EDGE of the screen (or at PATH from the target, if the edge is farther), so it
      is visible for its whole flight, wherever the target is. It never comes from below: that side would give
      the player no time to see it
    - Strikes the UI did not hear in advance (the player's own attacks, counters) are cut at once: they are
      drawn as a fast "instant" strike
* */
final class SlashEffect
{
    enum Outcome { HIT, PARRIED, DODGED }

    private static final long TRAVEL_MS = 800;        // the blade flies for this long at most before the impact
    private static final long MIN_TRAVEL_MS = 120;
    private static final long INSTANT_MS = 100;       // the cut of a strike that was not announced
    private static final long SCAR_MS = 350;
    private static final long SPARK_MS = 220;
    private static final long EXPIRE_MS = 600;        // a strike that never gets a verdict is dropped this late

    private static final double PATH = 480;           // the farthest an announced blade starts from the target (px)
    private static final double INSTANT_PATH = 320;
    private static final double MIN_PATH = 60;
    private static final double BLADE_LENGTH = 260;
    private static final double FOLLOW_SPEED = 1.2;   // after the impact the blade goes on at this speed (px per ms)

    // Where the blades come from (degrees, 0 = right, 90 = down, so 270 = from above). Never from below.
    // Taken in turn: the first two make a cross
    private static final double[] ANGLES = {215, 325, 270, 190, 350, 240, 300};

    private static final Color PARTY_COLOR = Color.web("#7fe0ff");
    private static final Color ENEMY_COLOR = Color.web("#ff5a4f");

    private final Pane layer;
    private final List<Strike> strikes = new ArrayList<>();
    private int nextAngle = 0;

    SlashEffect(Pane layer)
    {
        this.layer = Objects.requireNonNull(layer, "layer must not be null");
    }

    // A hit that is going to land on the target at impactMs (battle clock)
    void schedule(Fighter attacker, Fighter target, Node targetNode,
                  long startMs, long impactMs, StrikeWeight weight, boolean fromParty)
    {
        long travel = Math.max(MIN_TRAVEL_MS, Math.min(TRAVEL_MS, impactMs - startMs));

        add(new Strike(attacker, target, targetNode, startMs, impactMs, travel, PATH, halfWidth(weight), fromParty));
    }

    // The hit has been judged. If it was announced its strike finishes, otherwise a fast cut is drawn now
    void resolve(Fighter attacker, Fighter target, Node targetNode,
                 Outcome outcome, boolean critical, boolean fromParty, long nowMs)
    {
        Objects.requireNonNull(outcome, "outcome must not be null");

        for (Strike strike : strikes)
        {
            if (strike.attacker == attacker && strike.target == target && strike.outcome == null)
            {
                strike.outcome = outcome;
                strike.resolvedAtMs = Math.max(nowMs, strike.impactMs);
                return;
            }
        }

        instant(target, targetNode, outcome, critical, fromParty, nowMs);
    }

    // A cut that was not announced and always lands (a counter)
    void cut(Fighter target, Node targetNode, boolean critical, boolean fromParty, long nowMs)
    {
        instant(target, targetNode, Outcome.HIT, critical, fromParty, nowMs);
    }

    // Draws everything again for this moment of the battle clock
    void update(long nowMs)
    {
        double width = layer.getWidth();
        double height = layer.getHeight();
        if (width <= 0 || height <= 0) return;

        Iterator<Strike> iterator = strikes.iterator();
        while (iterator.hasNext())
        {
            Strike strike = iterator.next();

            if (!strike.draw(nowMs, width, height))
            {
                layer.getChildren().removeAll(strike.guide, strike.scar, strike.spark, strike.blade);
                iterator.remove();
            }
        }
    }

    // A new fight: nothing of the old one is left
    void clear()
    {
        layer.getChildren().clear();
        strikes.clear();
    }

    // ---------- Inside ----------

    private void instant(Fighter target, Node targetNode, Outcome outcome, boolean critical, boolean fromParty, long nowMs)
    {
        Strike strike = new Strike(null, target, targetNode, nowMs, nowMs + INSTANT_MS, INSTANT_MS,
                INSTANT_PATH, critical ? 20 : 13, fromParty);

        strike.outcome = outcome;
        strike.resolvedAtMs = strike.impactMs;
        add(strike);
    }

    private void add(Strike strike)
    {
        strikes.add(strike);
        layer.getChildren().addAll(strike.guide, strike.scar, strike.spark, strike.blade);
    }

    private static double halfWidth(StrikeWeight weight)
    {
        return switch (weight)
        {
            case LIGHT -> 9;
            case NORMAL -> 13;
            case HEAVY -> 18;
            case EXTREME -> 24;
        };
    }

    // How far a ray from (x, y) goes before it leaves a width x height rectangle
    private static double distanceToEdge(double x, double y, double dx, double dy, double width, double height)
    {
        double tx = dx > 1e-9 ? (width - x) / dx : dx < -1e-9 ? -x / dx : Double.POSITIVE_INFINITY;
        double ty = dy > 1e-9 ? (height - y) / dy : dy < -1e-9 ? -y / dy : Double.POSITIVE_INFINITY;

        return Math.max(0, Math.min(tx, ty));
    }

    // One blade: from where it starts, through the target, to the other side
    private final class Strike
    {
        private final Fighter attacker;     // null: a cut that was not announced
        private final Fighter target;
        private final Node targetNode;
        private final long startMs;
        private final long impactMs;
        private final long travelMs;
        private final double path;
        private final double halfWidth;
        private final Color color;
        private final double dx;            // from the target towards where the blade comes from
        private final double dy;

        private Outcome outcome = null;    // null: the hit has not been judged yet
        private long resolvedAtMs = 0;

        private final Line guide = new Line();
        private final Line scar = new Line();
        private final Circle spark = new Circle();
        private final Polygon blade = new Polygon();

        Strike(Fighter attacker, Fighter target, Node targetNode, long startMs, long impactMs,
               long travelMs, double path, double halfWidth, boolean fromParty)
        {
            this.attacker = attacker;
            this.target = target;
            this.targetNode = Objects.requireNonNull(targetNode, "targetNode must not be null");
            this.startMs = startMs;
            this.impactMs = impactMs;
            this.travelMs = travelMs;
            this.path = path;
            this.halfWidth = halfWidth;
            this.color = fromParty ? PARTY_COLOR : ENEMY_COLOR;

            double angle = Math.toRadians(ANGLES[nextAngle++ % ANGLES.length]);
            this.dx = Math.cos(angle);
            this.dy = Math.sin(angle);

            guide.setStroke(color.deriveColor(0, 1, 1, 0.55));
            guide.setStrokeWidth(3);
            scar.setStroke(Color.WHITE);
            scar.setStrokeWidth(5);
            scar.setEffect(new DropShadow(18, color));
            blade.setEffect(new DropShadow(26, color));
            spark.setFill(Color.TRANSPARENT);
            spark.setStroke(Color.web("#f1c40f"));
            spark.setStrokeWidth(4);

            guide.setVisible(false);
            scar.setVisible(false);
            spark.setVisible(false);
            blade.setVisible(false);
        }

        // Returns false when there is nothing left to draw
        boolean draw(long now, double width, double height)
        {
            // The layout bounds are the fixed box of the sprite: the cut aims at the same spot whatever the animation does
            Bounds bounds = layer.sceneToLocal(targetNode.localToScene(targetNode.getLayoutBounds()));
            if (bounds == null) return true;

            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY();
            double far = distanceToEdge(cx, cy, -dx, -dy, width, height);
            double begin = Math.max(MIN_PATH, Math.min(path, distanceToEdge(cx, cy, dx, dy, width, height)));

            boolean judged = outcome != null;
            boolean after = judged && now >= resolvedAtMs;      // the verdict is known and has been reached

            // A hit that never gets a verdict (the battle moved on): it just goes away
            if (!judged && now > impactMs + EXPIRE_MS) return false;

            // p: 0 = where the blade starts, 1 = on the target (it waits there if the verdict is late)
            // head: where the tip is on the line through the target (+ = towards where it came from)
            double p = 1.0;
            double head = 0;
            double alpha = 1.0;
            double sinceVerdict = 0;

            if (!after)
            {
                p = Math.min(1.0, (now - (impactMs - travelMs)) / (double) travelMs);
                head = begin * (1.0 - p);
            }
            else
            {
                sinceVerdict = now - resolvedAtMs;

                if (outcome == Outcome.PARRIED)
                {
                    head = FOLLOW_SPEED * sinceVerdict;                // it bounces back
                    alpha = 1.0 - sinceVerdict / SPARK_MS;
                }
                else
                {
                    head = -FOLLOW_SPEED * sinceVerdict;               // it goes on through
                    if (outcome == Outcome.DODGED) alpha = 0.55;
                }
            }

            boolean bladeDone = after && (outcome == Outcome.PARRIED ? alpha <= 0 : head < -(far + BLADE_LENGTH));
            boolean scarDone = outcome != Outcome.HIT || (after && sinceVerdict >= SCAR_MS);
            boolean sparkDone = outcome != Outcome.PARRIED || (after && sinceVerdict >= SPARK_MS);

            if (bladeDone && scarDone && sparkDone) return false;

            drawGuide(now, cx, cy, begin, after);
            drawBlade(cx, cy, head, p, alpha, bladeDone);
            drawScar(cx, cy, begin, far, after, sinceVerdict);
            drawSpark(cx, cy, after, sinceVerdict);

            return true;
        }

        // A faint line that tells where the blade will go, while it still waits to start
        private void drawGuide(long now, double cx, double cy, double begin, boolean after)
        {
            boolean show = attacker != null && !after && now >= startMs && now < impactMs;

            guide.setVisible(show);
            if (!show) return;

            guide.setStartX(cx + dx * begin);
            guide.setStartY(cy + dy * begin);
            guide.setEndX(cx);
            guide.setEndY(cy);
        }

        private void drawBlade(double cx, double cy, double head, double p, double alpha, boolean done)
        {
            boolean show = p >= 0 && alpha > 0 && !done;

            blade.setVisible(show);
            if (!show) return;

            double tipX = cx + dx * head;
            double tipY = cy + dy * head;
            double baseX = tipX + dx * BLADE_LENGTH;      // the tail is behind the tip
            double baseY = tipY + dy * BLADE_LENGTH;
            double nx = -dy * halfWidth;
            double ny = dx * halfWidth;

            blade.getPoints().setAll(tipX, tipY, baseX + nx, baseY + ny, baseX - nx, baseY - ny);
            blade.setFill(new LinearGradient(tipX, tipY, baseX, baseY, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.WHITE.deriveColor(0, 1, 1, alpha)),       // a white-hot tip
                    new Stop(0.3, color.deriveColor(0, 1, 1, alpha)),
                    new Stop(1, color.deriveColor(0, 1, 1, 0))));
        }

        // The cut that stays for a moment where the blade went through
        private void drawScar(double cx, double cy, double begin, double far, boolean after, double sinceVerdict)
        {
            boolean show = outcome == Outcome.HIT && after && sinceVerdict < SCAR_MS;

            scar.setVisible(show);
            if (!show) return;

            scar.setStartX(cx + dx * begin);
            scar.setStartY(cy + dy * begin);
            scar.setEndX(cx - dx * far);
            scar.setEndY(cy - dy * far);
            scar.setOpacity(1.0 - sinceVerdict / SCAR_MS);
        }

        // A ring that opens on the target when the blade is parried
        private void drawSpark(double cx, double cy, boolean after, double sinceVerdict)
        {
            boolean show = outcome == Outcome.PARRIED && after && sinceVerdict < SPARK_MS;

            spark.setVisible(show);
            if (!show) return;

            double u = sinceVerdict / SPARK_MS;
            spark.setCenterX(cx);
            spark.setCenterY(cy);
            spark.setRadius(14 + 70 * u);
            spark.setOpacity(1.0 - u);
        }
    }
}