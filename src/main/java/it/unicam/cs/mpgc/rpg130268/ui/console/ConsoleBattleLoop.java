package it.unicam.cs.mpgc.rpg130268.ui.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

// Runs a ConsoleBattle with the real time and the real keyboard. A thread reads the lines and stamps each one
// with the moment it arrived; the loop moves the battle forward up to that moment BEFORE handing the line over,
// so a key is stamped where it happened and not at the end of the frame
public final class ConsoleBattleLoop
{
    private static final long FRAME_MS = 16;
    private static final long NANOS_PER_MS = 1_000_000;

    private record TimedLine(String text, long atNanos) {}

    private ConsoleBattleLoop() { }

    public static void run(ConsoleBattle runner, InputStream in) throws InterruptedException
    {
        BlockingQueue<TimedLine> lines = new LinkedBlockingQueue<>();
        AtomicBoolean closed = new AtomicBoolean(false);

        Thread reader = new Thread(() ->
        {
            try (BufferedReader input = new BufferedReader(new InputStreamReader(in)))
            {
                String line;
                while ((line = input.readLine()) != null)
                    lines.add(new TimedLine(line, System.nanoTime()));
            }
            catch (IOException ignored)
            {
                // the input is gone: the same as if it had ended
            }
            finally
            {
                closed.set(true);
            }
        });
        reader.setDaemon(true);
        reader.start();

        runner.start();
        long last = System.nanoTime();

        while (!runner.isOver() && !(closed.get() && lines.isEmpty()))
        {
            long frameEnd = System.nanoTime();

            TimedLine line;
            while ((line = lines.peek()) != null && line.atNanos() <= frameEnd)
            {
                lines.poll();
                last = advanceTo(runner, last, line.atNanos());
                runner.input(line.text());
            }

            last = advanceTo(runner, last, frameEnd);
            Thread.sleep(FRAME_MS);
        }
    }

    // Moves the runner forward, in whole milliseconds, up to the instant "to". Returns the instant it reached
    // (the milliseconds that are left over are kept for the next time, so no time is lost)
    private static long advanceTo(ConsoleBattle runner, long last, long to)
    {
        long ms = (to - last) / NANOS_PER_MS;

        if (ms <= 0) return last;

        runner.advance(ms);
        return last + ms * NANOS_PER_MS;
    }
}