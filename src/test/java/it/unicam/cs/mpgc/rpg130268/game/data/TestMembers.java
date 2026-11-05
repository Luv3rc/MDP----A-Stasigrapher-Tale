package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.LevelRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;

import java.util.Map;
import java.util.Set;

// A member that is not Gunther, so that what is about "the party" and not about him can be tested with two of them
public final class TestMembers
{
    private static final class Squire extends PartyMember
    {
        Squire(it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatRules rules, it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints points,
               int unspent, int maxAp, int startingAp, int level, int xp)
        {
            super("Squire", rules, points, unspent, Set.of(WeaponType.SWORD), maxAp, startingAp, ApRules.defaults(),
                    LevelRules.defaults(), level, xp);
        }

        @Override protected void onLevelUp() { }
    }

    public static final MemberCatalog.MemberBuilder SQUIRE = Squire::new;

    // The members of the game and the squire
    public static Map<String, MemberCatalog.MemberBuilder> withSquire()
    {
        java.util.Map<String, MemberCatalog.MemberBuilder> builders = new java.util.HashMap<>(MemberCatalog.standardBuilders());
        builders.put("squire", SQUIRE);
        return builders;
    }

    // members.xml with Gunther and the squire, in this order, both in the party
    public static final String TWO_MEMBERS = "<members startingParty=\"gunther squire\">"
            + "<member id=\"gunther\" baseHealth=\"164\" healthPerPoint=\"15\" pointsPerLevel=\"3\" statCap=\"60\"><ap max=\"9\" start=\"3\"/>"
            + "<weapon ref=\"thesmosia\"/><abilities><learn ref=\"basic-attack\"/><learn ref=\"cuts-for-ink\"/></abilities></member>"
            + "<member id=\"squire\" baseHealth=\"100\" healthPerPoint=\"10\" pointsPerLevel=\"3\" statCap=\"60\"><ap max=\"6\" start=\"2\"/>"
            + "<weapon ref=\"thesmosia\"/><abilities><learn ref=\"basic-attack\"/></abilities></member></members>";

    private TestMembers() { }
}
