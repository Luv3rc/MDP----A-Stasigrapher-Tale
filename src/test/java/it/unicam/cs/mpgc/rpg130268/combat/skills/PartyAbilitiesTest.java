package it.unicam.cs.mpgc.rpg130268.combat.skills;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.LevelRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.BasicAttack;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.PartyAbilities;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.CutsForInk;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PartyAbilitiesTest
{
    // A member that is not Gunther, to see what an ability that is only for him does
    private static final class Squire extends PartyMember
    {
        Squire()
        {
            super("Squire", new Stats(100, 5, 5, 5, 5), Set.of(WeaponType.SWORD), 9, 3, ApRules.defaults(), LevelRules.defaults(), 1, 0);
        }

        @Override protected void onLevelUp() { }
    }

    private static Gunther gunther() { return new Gunther(new Stats(100, 10, 4, 6, 3), 9, 3, ApRules.defaults(), InkRules.defaults()); }

    @Test void theAbilitiesOfTheGameAreAllThere()
    {
        assertEquals(Set.of("basic-attack", "cuts-for-ink", "stasigraphic-fury", "thirst-for-ink"), PartyAbilities.standard().ids());
    }

    @Test void everyIdMakesTheAbilityWithThatSameId()
    {
        PartyAbilities abilities = PartyAbilities.standard();
        Gunther gunther = gunther();

        for (String id : abilities.ids())
            assertEquals(id, abilities.createFor(id, gunther).getId());
    }

    @Test void theAbilityIsMadeForTheMemberWhoAsksForIt()
    {
        Gunther gunther = gunther();

        assertInstanceOf(CutsForInk.class, PartyAbilities.standard().createFor("cuts-for-ink", gunther));
    }

    @Test void anAbilityForAnyMemberCanBeMadeForAnyMember()
    {
        assertInstanceOf(BasicAttack.class, PartyAbilities.standard().createFor("basic-attack", new Squire()));
    }

    @Test void anAbilityOfGuntherCannotBeMadeForAnotherMember()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PartyAbilities.standard().createFor("cuts-for-ink", new Squire()));

        assertTrue(e.getMessage().contains("Gunther"));
    }

    @Test void anAbilityThatDoesNotExistIsRefused()
    {
        assertThrows(IllegalArgumentException.class, () -> PartyAbilities.standard().createFor("fireball", gunther()));
        assertFalse(PartyAbilities.standard().contains("fireball"));
    }

    @Test void anIdCannotBeRegisteredTwiceOrBadlyWritten()
    {
        PartyAbilities abilities = PartyAbilities.standard();

        assertThrows(IllegalArgumentException.class, () -> abilities.register("basic-attack", PartyMember.class, BasicAttack::new));
        assertThrows(IllegalArgumentException.class, () -> abilities.register("Basic Attack", PartyMember.class, BasicAttack::new));
    }
}
