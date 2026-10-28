package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/* *
    The animator: a character that moves. It holds several animations (each one is a SpriteSheet) and plays one at a
    time: "idle" in a loop, and the others ("attack", "hit", "dodge", "parry", "defeat"...) once, then back to idle.
    A still picture is just an idle of one frame.

    THE BOX IS FIXED. The sprite takes a place on the screen (width x height):
    - The character standing (the idle animation) is scaled, keeping its proportions, to fill the box as much as it
      can, whatever the size of the art is: art that is too big is made smaller, art that is too small is made bigger
    - Every animation of the character has that same size, and they are placed so that the character does not move
      when one starts: it stands on the bottom of the box, in the middle. An attack that needs more room (a long
      sword) goes out of the box a bit, but never more than OVERFLOW times the box: past that, the size of ALL the
      animations is reduced to fit. What would be even farther is cut
    - The size does NOT depend on the animation that needs the most room, or a wide attack would make the character
      look tiny when it stands

    The art is supposed to look to the RIGHT. A character that must look to the left is mirrored.
    It is time that moves the frames (a JavaFX Transition), not the drawing: a slow computer skips frames and the
    animation lasts the same
* */
final class AnimatedSprite extends StackPane
{
    static final String IDLE = "idle";
    static final double DEFAULT_FPS = 8;

    private static final double OVERSAMPLING = 2.0;      // how many texels of the prepared picture for one pixel of the layout
    private static final double OVERFLOW = 1.7;          // how many boxes an animation can take, at most (width and height)

    // One animation: its frames, how fast it goes, and if it goes on after the last frame
    private static final class Clip
    {
        final SpriteSheet sheet;
        final double fps;
        final boolean loops;
        double scale = 1;               // the screen pixels of one pixel of the art, set by layOut()
        double shiftX = 0;              // where it has to be moved so that the standing character does not move
        double shiftY = 0;

        Clip(SpriteSheet sheet, double fps, boolean loops)
        {
            this.sheet = sheet;
            this.fps = fps;
            this.loops = loops;
        }

        double durationMs() { return 1000.0 * sheet.getFrames() / fps; }
    }

    private final double width;
    private final double height;
    private final ImageView view = new ImageView();
    private final Map<String, Clip> clips = new HashMap<>();
    private final boolean mirrored;

    private boolean laidOut = false;
    private Animation running = null;

    AnimatedSprite(double width, double height, boolean mirrored)
    {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("the size must be positive");

        this.width = width;
        this.height = height;
        this.mirrored = mirrored;

        view.setSmooth(true);                               // the picture is already made of big blocks (SpriteSheet)
        view.setPreserveRatio(false);                       // the size is calculated here
        view.setScaleX(mirrored ? -1 : 1);

        setAlignment(Pos.BOTTOM_CENTER);
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
        double extraX = width * (OVERFLOW - 1) / 2;
        double extraY = height * (OVERFLOW - 1);
        setClip(new Rectangle(-extraX, -extraY, width + 2 * extraX, height + extraY));    // what is far outside is cut
        getChildren().add(view);
    }

    // Adds an animation. All of them must be added before the first play()
    void add(String name, SpriteSheet sheet, double fps, boolean loops)
    {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(sheet, "sheet must not be null");

        if (fps <= 0)
            throw new IllegalArgumentException("fps must be positive");
        if (laidOut)
            throw new IllegalStateException("the animations must be added before the first play()");

        clips.put(name, new Clip(sheet, fps, loops));
    }

    boolean has(String name) { return clips.containsKey(name); }

    // How long the animation lasts (0 if there is no such animation)
    double durationMs(String name)
    {
        Clip clip = clips.get(name);
        return clip == null ? 0 : clip.durationMs();
    }

    // The animation starts from the beginning and goes on until it is told otherwise (or until its last frame, if it
    // does not loop: then the last frame stays)
    void play(String name)
    {
        Clip clip = clips.get(name);
        if (clip == null) return;

        start(clip, null);
    }

    // Once, and then back to idle. If there is no such animation, nothing happens
    void playOnce(String name)
    {
        Clip clip = clips.get(name);
        if (clip == null) return;

        start(clip, () -> play(IDLE));
    }

    void stop()
    {
        if (running != null) running.stop();
        running = null;
    }

    // ---------- Inside ----------

    private void start(Clip clip, Runnable then)
    {
        stop();
        layOut();

        if (clip.sheet.getFrames() == 1 && clip.loops)      // a still picture: nothing has to run
        {
            show(clip, 0);
            return;
        }

        Transition animation = new Transition()
        {
            private int shown = -1;

            {
                setCycleDuration(Duration.millis(clip.durationMs()));
                setInterpolator(Interpolator.LINEAR);
                setCycleCount(clip.loops ? Animation.INDEFINITE : 1);
            }

            @Override
            protected void interpolate(double fraction)
            {
                int frame = Math.min(clip.sheet.getFrames() - 1, (int) (fraction * clip.sheet.getFrames()));

                if (frame != shown)
                {
                    shown = frame;
                    show(clip, frame);
                }
            }
        };

        if (then != null)
            animation.setOnFinished(event -> then.run());

        running = animation;
        show(clip, 0);
        animation.play();
    }

    private void show(Clip clip, int frame)
    {
        view.setImage(clip.sheet.getPrepared());
        view.setViewport(clip.sheet.viewport(frame));
        view.setFitWidth(clip.sheet.getCropWidth() * clip.scale);
        view.setFitHeight(clip.sheet.getCropHeight() * clip.scale);
        view.setTranslateX(clip.shiftX);
        view.setTranslateY(clip.shiftY);
    }

    // Decides, once, what is shown of the frames and how big, the same for every animation (when their frames have the
    // same size, as they should): the part that ANY of them uses is shown, but the SIZE is the one that makes the
    // standing character fill the box (limited so that the animations do not take more than OVERFLOW boxes)
    private void layOut()
    {
        if (laidOut) return;
        laidOut = true;

        Clip standing = clips.containsKey(IDLE) ? clips.get(IDLE) : clips.values().iterator().next();
        Rectangle2D stands = standing.sheet.getUsed();         // where the standing character is, in a frame

        Rectangle2D common = stands;
        for (Clip clip : clips.values())
            if (sameFrame(standing.sheet, clip.sheet))
                common = union(common, clip.sheet.getUsed());

        double scale = Math.min(width / stands.getWidth(), height / stands.getHeight());
        scale = Math.min(scale, Math.min(width * OVERFLOW / common.getWidth(), height * OVERFLOW / common.getHeight()));

        for (Clip clip : clips.values())
        {
            if (sameFrame(standing.sheet, clip.sheet))
            {
                clip.scale = scale;

                // The standing character must stay where it is: the middle of its feet on the middle of the bottom
                double dx = ((stands.getMinX() + stands.getWidth() / 2) - (common.getMinX() + common.getWidth() / 2)) * scale;
                clip.shiftX = mirrored ? dx : -dx;
                clip.shiftY = (common.getMaxY() - stands.getMaxY()) * scale;

                clip.sheet.prepare(common, magnification(scale));
            }
            else
            {
                Rectangle2D own = clip.sheet.getUsed();           // a different size of frame: on its own
                clip.scale = Math.min(width / own.getWidth(), height / own.getHeight());
                clip.sheet.prepare(own, magnification(clip.scale));
            }
        }
    }

    // Art that is shown bigger than it is gets every pixel multiplied first; big art stays as it is
    private static int magnification(double scale)
    {
        return scale > 1 ? (int) Math.ceil(scale * OVERSAMPLING) : 1;
    }

    private static boolean sameFrame(SpriteSheet a, SpriteSheet b)
    {
        return Math.abs(a.getFrameWidth() - b.getFrameWidth()) < 0.5 && Math.abs(a.getFrameHeight() - b.getFrameHeight()) < 0.5;
    }

    private static Rectangle2D union(Rectangle2D a, Rectangle2D b)
    {
        double minX = Math.min(a.getMinX(), b.getMinX());
        double minY = Math.min(a.getMinY(), b.getMinY());
        double maxX = Math.max(a.getMaxX(), b.getMaxX());
        double maxY = Math.max(a.getMaxY(), b.getMaxY());

        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }
}