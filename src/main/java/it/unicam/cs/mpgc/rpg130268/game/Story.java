package it.unicam.cs.mpgc.rpg130268.game;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// Steps in the order they are played, each with its id. It has to finish with an EndStep
// and the ids can't repeat--Saves remeber the id of the last step
public record Story(List<StoryNode> nodes)
{
    public Story
    {
        Objects.requireNonNull(nodes, "nodes must not be null");

        if (nodes.isEmpty())
            throw new IllegalArgumentException("a story needs at least one step");

        nodes = List.copyOf(nodes);
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < nodes.size(); i++)
        {
            StoryNode node = nodes.get(i);
            boolean last = i == nodes.size() - 1;

            if (!ids.add(node.id()))
                throw new IllegalArgumentException("the id '" + node.id() + "' is used twice");

            if (node.step() instanceof StoryStep.RewardStep || node.step() instanceof StoryStep.DefeatStep)
                throw new IllegalArgumentException("reward and defeat steps are made by the game, not written in the story");

            if (node.step() instanceof StoryStep.EndStep && !last)
                throw new IllegalArgumentException("nothing can come after the end");

            if (last && !(node.step() instanceof StoryStep.EndStep))
                throw new IllegalArgumentException("the story must finish with an EndStep");
        }
    }

    public StoryNode node(int index) { return nodes.get(index); }

    public boolean contains(String id) { return nodes.stream().anyMatch(node -> node.id().equals(id)); }

    public int indexOf(String id)
    {
        for (int i = 0; i < nodes.size(); i++)
            if (nodes.get(i).id().equals(id))
                return i;

        throw new IllegalArgumentException("there is no step '" + id + "' in this story");
    }
}
