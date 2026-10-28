package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.util.Arrays;
import java.util.Objects;

/* *
    A strip of frames: one picture with all the frames of an animation, one after the other, from left to right.
    The frames are squares (a strip 960 x 96 has 10 frames of 96 x 96), unless the number of frames is told.
    A still picture is a strip of one frame.

    Two things make small art look good when it is shown big:
    - Drawn art usually fills only a small part of its frame (a character of 23 x 34 pixels in a frame of 96 x 96).
      The sheet finds the part of the frame that is really used (without counting the faint pixels: a soft shadow
      must not make the character look smaller), looking at ALL the frames, so the character can be shown as big as
      its place allows, and does not jump from a frame to the next one
    - Before it is shown, every pixel of the art is made a block of k x k identical pixels (no blur, no
      invention). Then the screen shrinks that big picture to the size it needs, and every pixel of the art comes out
      with sharp and even edges. Stretching the small picture directly would make some pixels wider than others
* */
final class SpriteSheet
{
    private static final int MAX_SIDE = 4096;     // no picture made here is wider or taller than this
    private static final int MIN_ALPHA = 40;      // out of 255: fainter pixels (a soft shadow, a glow) are not "drawn"

    private final Image image;
    private final int frames;
    private final double frameWidth;
    private final Rectangle2D used;      // the part of a frame where something is drawn (in the pixels of the art)

    // What prepare() made
    private Image prepared = null;
    private Rectangle2D crop = null;
    private int magnification = 1;

    // Square frames: the number of them is the width divided by the height
    static SpriteSheet of(Image image)
    {
        Objects.requireNonNull(image, "image must not be null");

        int frames = (int) Math.max(1, Math.round(image.getWidth() / image.getHeight()));
        return new SpriteSheet(image, frames);
    }

    SpriteSheet(Image image, int frames)
    {
        this.image = Objects.requireNonNull(image, "image must not be null");

        if (frames < 1)
            throw new IllegalArgumentException("a sheet has at least one frame");

        this.frames = frames;
        this.frameWidth = image.getWidth() / frames;
        this.used = findUsedPart();
    }

    int getFrames() { return frames; }

    double getFrameWidth() { return frameWidth; }

    double getFrameHeight() { return image.getHeight(); }

    // The part of a frame where something is drawn, in the pixels of the art
    Rectangle2D getUsed() { return used; }

    /* *
        Gets the sheet ready to be shown: every frame is cut to crop (a rectangle inside a frame, in the pixels of
        the art) and every pixel of the art becomes a block of magnification x magnification. The magnification is
        lowered if the picture would be too big
    * */
    void prepare(Rectangle2D crop, int magnification)
    {
        Objects.requireNonNull(crop, "crop must not be null");

        int cropWidth = (int) crop.getWidth();
        int cropHeight = (int) crop.getHeight();
        int k = Math.max(1, Math.min(magnification, Math.min(MAX_SIDE / (frames * cropWidth), MAX_SIDE / cropHeight)));

        this.crop = crop;
        PixelReader reader = image.getPixelReader();

        if (reader == null || k == 1)
        {
            this.prepared = image;           // shown as it is: the viewport finds the frames
            this.magnification = 1;
            return;
        }

        int width = frames * cropWidth * k;
        int height = cropHeight * k;
        int[] out = new int[width * height];

        for (int frame = 0; frame < frames; frame++)
        {
            int left = (int) Math.round(frame * frameWidth) + (int) crop.getMinX();

            for (int y = 0; y < cropHeight; y++)
            {
                for (int x = 0; x < cropWidth; x++)
                {
                    int sourceX = left + x;
                    int sourceY = (int) crop.getMinY() + y;

                    int argb = (sourceX < image.getWidth() && sourceY < image.getHeight()) ? reader.getArgb(sourceX, sourceY) : 0;
                    if (argb == 0) continue;

                    for (int dy = 0; dy < k; dy++)
                    {
                        int start = (y * k + dy) * width + (frame * cropWidth + x) * k;
                        Arrays.fill(out, start, start + k, argb);
                    }
                }
            }
        }

        WritableImage big = new WritableImage(width, height);
        big.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), out, 0, width);

        this.prepared = big;
        this.magnification = k;
    }

    boolean isPrepared() { return prepared != null; }

    // The picture to show (after prepare)
    Image getPrepared() { return prepared; }

    // The size of what is shown, in the pixels of the art (after prepare)
    double getCropWidth() { return crop.getWidth(); }

    double getCropHeight() { return crop.getHeight(); }

    // The part of the prepared picture where a frame is (0 = the first)
    Rectangle2D viewport(int frame)
    {
        int index = Math.max(0, Math.min(frames - 1, frame));

        if (magnification == 1)
            return new Rectangle2D(Math.round(index * frameWidth) + crop.getMinX(), crop.getMinY(), crop.getWidth(), crop.getHeight());

        double width = crop.getWidth() * magnification;
        return new Rectangle2D(index * width, 0, width, crop.getHeight() * magnification);
    }

    // ---------- Inside ----------

    // The smallest rectangle that holds every pixel that is not transparent, in every frame
    private Rectangle2D findUsedPart()
    {
        PixelReader reader = image.getPixelReader();
        int width = (int) Math.round(frameWidth);
        int height = (int) image.getHeight();

        if (reader == null)
            return new Rectangle2D(0, 0, width, height);

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;

        for (int frame = 0; frame < frames; frame++)
        {
            int left = (int) Math.round(frame * frameWidth);

            for (int y = 0; y < height; y++)
            {
                for (int x = 0; x < width && left + x < image.getWidth(); x++)
                {
                    if (((reader.getArgb(left + x, y) >>> 24) & 0xFF) < MIN_ALPHA) continue;

                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < 0)       // nothing is drawn: the whole frame
            return new Rectangle2D(0, 0, width, height);

        return new Rectangle2D(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}