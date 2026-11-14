package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.animation.AnimationTimer;

import java.util.Objects;
import java.util.function.LongConsumer;

// The frames of a battle. At every frame the time that has passed is given to "advance", then "render" draws and
// "afterFrame" is told the time
final class BattleLoop
{
    private static final long NANOS_PER_MS = 1_000_000;
    private static final long MAX_STEP_MS = 100;

    private final LongConsumer advance;
    private final Runnable render;
    private final LongConsumer afterFrame;

    private final AnimationTimer timer = new AnimationTimer()
    {
        @Override
        public void handle(long ignored) { catchUp(); }
    };

    private long lastNanos;

    BattleLoop(LongConsumer advance, Runnable render, LongConsumer afterFrame)
    {
        this.advance = Objects.requireNonNull(advance, "advance must not be null");
        this.render = Objects.requireNonNull(render, "render must not be null");
        this.afterFrame = Objects.requireNonNull(afterFrame, "afterFrame must not be null");
    }

    void start()
    {
        timer.stop();
        lastNanos = System.nanoTime();
        timer.start();
    }

    void stop() { timer.stop(); }

    void catchUp()
    {
        long nowNanos = System.nanoTime();
        long elapsedMs = (nowNanos - lastNanos) / NANOS_PER_MS;

        if (elapsedMs > 0)
        {
            lastNanos += elapsedMs * NANOS_PER_MS;
            advance.accept(Math.min(elapsedMs, MAX_STEP_MS));
        }

        render.run();
        afterFrame.accept(nowNanos);
    }
}
