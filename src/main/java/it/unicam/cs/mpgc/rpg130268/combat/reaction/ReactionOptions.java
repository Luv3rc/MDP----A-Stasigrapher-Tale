package it.unicam.cs.mpgc.rpg130268.combat.reaction;

// Which reactions a hit accepts. NONE: the hit cannot be reacted to at all (e.g. the player's own strikes)
public enum ReactionOptions
{
    PARRY_ONLY, DODGE_ONLY, PARRY_OR_DODGE, NONE;

    public boolean allows (ReactionType type)
    {
        return switch (this)
        {
            case PARRY_ONLY -> type == ReactionType.PARRY;
            case DODGE_ONLY -> type == ReactionType.DODGE;
            case PARRY_OR_DODGE -> type == ReactionType.PARRY || type == ReactionType.DODGE;
            case NONE -> false;
        };
    }
}
