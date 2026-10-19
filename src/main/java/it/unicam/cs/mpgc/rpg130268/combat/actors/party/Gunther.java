package it.unicam.cs.mpgc.rpg130268.combat.actors.party;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.LevelRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkPool;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.CutsForInk;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.StasigraphicFury;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.ThirstForInk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// TODO
public final class Gunther extends PartyMember
{
    private final InkPool ink;
    private final InkRules inkRules;
    private final List<Ability> abilities = new ArrayList<>();

    public Gunther(Stats stats, int maxAp, int startingAp, ApRules apRules, InkRules inkRules)
    {
        super("Gunther", stats, maxAp, startingAp, apRules, LevelRules.defaults(), 1, 0);
        this.ink = new InkPool(8, 0, inkRules);
        this.inkRules = inkRules;
        this.learn(CutsForInk::new);
        this.learn(StasigraphicFury::new);
        this.learn(ThirstForInk::new);
    }

    public InkPool getInk() { return ink; }
    public InkRules getInkRules() { return inkRules; }
    @Override
    public List<Ability> getAbilities() { return List.copyOf(abilities); }

    // This is to add the possibility of not knowing all the abilities from the start
    public void learn(Function<Gunther, Ability> factory)
    {
        Ability ability = factory.apply(this);
        boolean alreadyKnown = abilities.stream()
                .anyMatch(a -> a.getName().equals(ability.getName()));

        if (!alreadyKnown) abilities.add(ability);
    }

    @Override
    protected void onLevelUp()
    {
        // TODO
    }

    @Override
    public void onBattleStart() {
        super.onBattleStart();
        ink.resetInk();
    }

    @Override
    public void onBasicAttackLanded() {
        super.onBasicAttackLanded();
        ink.gainInk(inkRules.inkGainPerHit());
    }
}
