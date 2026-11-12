package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilityCatalog;
import it.unicam.cs.mpgc.rpg130268.game.Story;

import java.util.Objects;

public record GameData(WeaponCatalog weapons, MemberCatalog members, EncounterCatalog encounters, Story story)
{
    public static final String WEAPONS_FILE = "/game/weapons.xml";
    public static final String MEMBERS_FILE = "/game/members.xml";
    public static final String ENEMIES_FILE = "/game/enemies.xml";
    public static final String STORY_FILE = "/game/story.xml";

    public GameData
    {
        Objects.requireNonNull(weapons, "weapons must not be null");
        Objects.requireNonNull(members, "members must not be null");
        Objects.requireNonNull(encounters, "encounters must not be null");
        Objects.requireNonNull(story, "story must not be null");
    }

    public static GameData bundled()
    {
        WeaponCatalog weapons = WeaponCatalog.fromResource(WEAPONS_FILE);
        MemberCatalog members = MemberCatalog.fromResource(MEMBERS_FILE, weapons, PartyAbilityCatalog.standard());
        EncounterCatalog encounters = EncounterCatalog.fromResource(ENEMIES_FILE);

        return new GameData(weapons, members, encounters, StoryLoader.fromResource(STORY_FILE, encounters));
    }
}
