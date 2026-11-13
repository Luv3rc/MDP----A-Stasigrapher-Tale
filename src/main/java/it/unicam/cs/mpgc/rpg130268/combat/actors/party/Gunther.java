package it.unicam.cs.mpgc.rpg130268.combat.actors.party;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.LevelRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ResourceGauge;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkPool;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.BasicAttack;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.CutsForInk;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.StasigraphicFury;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.ThirstForInk;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

// TODO
public final class Gunther extends PartyMember
{

    private final InkPool ink;
    private final InkRules inkRules;

    // A Gunther with ready-made stats (tests, demo battles)
    public Gunther(Stats stats, int maxAp, int startingAp, ApRules apRules, InkRules inkRules)
    {
        super("Gunther", stats, Set.of(WeaponType.SWORD), maxAp, startingAp,
                apRules, LevelRules.defaults(), 1, 0);
        this.ink = new InkPool(8, 0, inkRules);
        this.inkRules = inkRules;

        learnTheStartingAbilities();
    }

    // The Gunther of the game, at any point of it (a new game, or the one a save brings back)
    // The abilities he has come from members.xml (or from the save), one learn() each
    public Gunther(StatRules statRules, StatPoints statPoints, int unspentPoints, int maxAp, int startingAp,
                   ApRules apRules, InkRules inkRules, int startingLevel, int startingXp)
    {
        super("Gunther", statRules, statPoints, unspentPoints, Set.of(WeaponType.SWORD), maxAp, startingAp,
                apRules, LevelRules.defaults(), startingLevel, startingXp);
        this.ink = new InkPool(8, 0, inkRules);
        this.inkRules = inkRules;
    }

    private void learnTheStartingAbilities()
    {
        this.learn(BasicAttack::new);

        this.learn(CutsForInk::new);
        this.learn(StasigraphicFury::new);
        this.learn(ThirstForInk::new);
    }

    public InkPool getInk() { return ink; }
    public InkRules getInkRules() { return inkRules; }

    @Override
    public Optional<ResourceGauge> getExtraGauge()
    {
        return Optional.of(new ResourceGauge("INK", ink.getCurrentInk(), ink.getMaxInk()));
    }

    public void learn(Function<Gunther, Ability> factory)
    {
        learn(factory.apply(this));
    }

    @Override
    protected void onLevelUp()
    {
        // Todo
    }

    @Override
    public void onBattleStart()
    {
        super.onBattleStart();
        ink.resetInk();
    }

    @Override
    public void onBasicAttackLanded()
    {
        super.onBasicAttackLanded();
        ink.gainInk(inkRules.inkGainPerHit());
    }
}
