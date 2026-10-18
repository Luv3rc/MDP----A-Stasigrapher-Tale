package it.unicam.cs.mpgc.rpg130268.combat.reaction;

// Which reactions a hit accepts
public enum ReactionOptions
{
    PARRY_ONLY, DODGE_ONLY, PARRY_OR_DODGE;

    public boolean allows (ReactionType type)
    {
        return switch (this)
        {
            case PARRY_ONLY -> type == ReactionType.PARRY;
            case DODGE_ONLY -> type == ReactionType.DODGE;
            case PARRY_OR_DODGE -> type == ReactionType.PARRY || type == ReactionType.DODGE;
        };
    }
}
