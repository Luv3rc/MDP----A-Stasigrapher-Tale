package it.unicam.cs.mpgc.rpg130268.combat.reaction;

import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReactionResolverTest
{
    // Default windows: parry 160 ms (120 early, 40 late), dodge 300 ms (225 early, 75 late)
    private final ReactionResolver resolver = new ReactionResolver(ReactionWindows.defaults());
    private final Hit hit = new Hit(1000, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.HEAVY);

    private ReactionOutcome parryAt(long ms) { return resolver.resolve(new ReactionInput(ReactionType.PARRY, ms), hit); }
    private ReactionOutcome dodgeAt(long ms) { return resolver.resolve(new ReactionInput(ReactionType.DODGE, ms), hit); }

    // ---------- Parry ----------

    @Test void parryExactlyOnTheImpactSucceeds()
    {
        ReactionOutcome out = parryAt(1000);

        assertTrue(out.isSuccessfulParry());
        assertTrue(out.avoidsDamage());
    }

    @Test void parryWorksShortlyBeforeAndShortlyAfterTheImpact()
    {
        assertTrue(parryAt(950).isSuccessfulParry());
        assertTrue(parryAt(1020).isSuccessfulParry());
    }

    @Test void parryEarlyEdgeIsInclusive()
    {
        assertTrue(parryAt(880).isSuccessfulParry());      // 120 ms early
        assertFalse(parryAt(879).isSuccessfulParry());     // 121 ms early
    }

    @Test void parryLateEdgeIsInclusive()
    {
        assertTrue(parryAt(1040).isSuccessfulParry());     // 40 ms late
        assertFalse(parryAt(1041).isSuccessfulParry());    // 41 ms late
        assertEquals(ReactionQuality.MISS, parryAt(1041).quality());
    }

    @Test void parryWindowIsNotCentered()
    {
        assertTrue(parryAt(940).isSuccessfulParry());      // 60 ms early: fine
        assertFalse(parryAt(1060).isSuccessfulParry());    // 60 ms late: too late
    }

    @Test
    void parryFarFromTheImpactFails()
    {
        assertFalse(parryAt(200).avoidsDamage());          // way too early
        assertFalse(parryAt(2000).avoidsDamage());         // way too late
    }

    // ---------- Dodge ----------

    @Test void dodgeInsideTheParryWindowIsPerfect()
    {
        assertEquals(ReactionQuality.PERFECT, dodgeAt(1000).quality());
        assertEquals(ReactionQuality.PERFECT, dodgeAt(880).quality());
        assertEquals(ReactionQuality.PERFECT, dodgeAt(1040).quality());
    }

    @Test void dodgeInTheRestOfItsWindowIsGood()
    {
        assertEquals(ReactionQuality.GOOD, dodgeAt(879).quality());    // just outside the parry window
        assertEquals(ReactionQuality.GOOD, dodgeAt(775).quality());    // 225 ms early, edge
        assertEquals(ReactionQuality.GOOD, dodgeAt(1041).quality());
        assertEquals(ReactionQuality.GOOD, dodgeAt(1075).quality());   // 75 ms late, edge
    }

    @Test void dodgeJustOutsideItsWindowFails()
    {
        assertEquals(ReactionQuality.MISS, dodgeAt(774).quality());
        assertEquals(ReactionQuality.MISS, dodgeAt(1076).quality());
    }

    @Test void dodgeWindowIsNotCenteredEither()
    {
        assertTrue(dodgeAt(800).avoidsDamage());           // 200 ms early: fine
        assertFalse(dodgeAt(1200).avoidsDamage());         // 200 ms late: too late
    }

    @Test void perfectAndGoodDodgesBothAvoidDamage()
    {
        assertTrue(dodgeAt(1000).avoidsDamage());
        assertTrue(dodgeAt(1060).avoidsDamage());
        assertFalse(dodgeAt(1000).isSuccessfulParry());    // a dodge is never a parry
    }

    // ---------- Not reacting, or reacting wrongly ----------

    @Test void noReactionIsAMiss()
    {
        ReactionOutcome out = resolver.resolve(ReactionInput.none(), hit);

        assertEquals(ReactionType.NONE, out.type());
        assertEquals(ReactionQuality.MISS, out.quality());
        assertFalse(out.avoidsDamage());
    }

    @Test void dodgingAParryOnlyHitIsAMissEvenWithPerfectTiming()
    {
        Hit parryOnly = new Hit(1000, ReactionOptions.PARRY_ONLY, StrikeWeight.HEAVY);
        ReactionOutcome out = resolver.resolve(new ReactionInput(ReactionType.DODGE, 1000), parryOnly);

        assertEquals(ReactionType.DODGE, out.type());      // remembers what the player pressed
        assertFalse(out.avoidsDamage());
    }

    @Test void parryingADodgeOnlyHitIsAMissEvenWithPerfectTiming()
    {
        Hit dodgeOnly = new Hit(1000, ReactionOptions.DODGE_ONLY, StrikeWeight.HEAVY);
        ReactionOutcome out = resolver.resolve(new ReactionInput(ReactionType.PARRY, 1000), dodgeOnly);

        assertFalse(out.isSuccessfulParry());
        assertFalse(out.avoidsDamage());
    }

    // ---------- Windows are configurable ----------

    @Test void widerWindowsAreMoreForgiving()
    {
        ReactionResolver easy = new ReactionResolver(new ReactionWindows(250, 400));

        // 180 ms early: too early for the default parry (120), fine for 250 * 3/4 = 187
        assertTrue(easy.resolve(new ReactionInput(ReactionType.PARRY, 820), hit).isSuccessfulParry());
        assertFalse(parryAt(820).isSuccessfulParry());
    }

    @Test void missingWindowsThrow()
    {
        assertThrows(NullPointerException.class, () -> new ReactionResolver(null));
    }
}
