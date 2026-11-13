package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.game.StoryStep;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class StepHandlers<R>
{
    private final Map<Class<? extends StoryStep>, Function<StoryStep, R>> handlers = new LinkedHashMap<>();

    public <S extends StoryStep> StepHandlers<R> on(Class<S> type, Function<? super S, ? extends R> handler)
    {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(handler, "handler must not be null");

        if (handlers.putIfAbsent(type, step -> handler.apply(type.cast(step))) != null)
            throw new IllegalArgumentException(type.getSimpleName() + " already has a handler");

        return this;
    }

    public R handle(StoryStep step)
    {
        Objects.requireNonNull(step, "step must not be null");

        Function<StoryStep, R> handler = handlers.get(step.getClass());

        if (handler == null)
            throw new IllegalStateException("no handler for " + step.getClass().getSimpleName());

        return handler.apply(step);
    }

    public Set<Class<? extends StoryStep>> missing()
    {
        Set<Class<? extends StoryStep>> missing = new LinkedHashSet<>();

        for (Class<?> kind : StoryStep.class.getPermittedSubclasses())
        {
            @SuppressWarnings("unchecked")
            Class<? extends StoryStep> step = (Class<? extends StoryStep>) kind;

            if (!handlers.containsKey(step))
                missing.add(step);
        }

        return missing;
    }

    public void requireAll()
    {
        Set<Class<? extends StoryStep>> missing = missing();

        if (!missing.isEmpty())
            throw new IllegalStateException("these steps have no handler: "
                    + missing.stream().map(Class::getSimpleName).toList());
    }
}
