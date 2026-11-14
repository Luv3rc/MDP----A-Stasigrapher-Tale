package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.Fighter;
import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ResourceGauge;

// A fighter. The HP bar follows what has been SHOWN, not the real HP
// (BattlePresenter changes it when the event that hurts or heals is shown)
public final class FighterView
{
    private final Fighter fighter;
    private final int maxHp;
    private int shownHp;

    FighterView(Fighter fighter)
    {
        this.fighter = fighter;
        this.maxHp = fighter.getStats().maxHealth();
        this.shownHp = fighter.getHp();
    }

    public Fighter getFighter() { return fighter; }
    public String getName() { return fighter.getName(); }
    public String getArtName() { return fighter.getArtName(); }
    public int getMaxHp() { return maxHp; }
    public int getShownHp() { return shownHp; }
    public boolean isAlive() { return shownHp > 0; }
    public boolean isPartyMember() { return fighter instanceof PartyMember; }

    public int getAp() { return fighter instanceof PartyMember member ? member.getAp().getCurrentAp() : 0; }
    public int getMaxAp() { return fighter instanceof PartyMember member ? member.getAp().getMaxAp() : 0; }

    // The extra bar (Ink for Gunther). It is 0 / 0 for a fighter that has none. Real values, like AP
    public int getInk() { return fighter.getExtraGauge().map(ResourceGauge::current).orElse(0); }
    public int getMaxInk() { return fighter.getExtraGauge().map(ResourceGauge::max).orElse(0); }

    public int getLevel()
    {
        if (fighter instanceof PartyMember member) return member.getLevel();
        if (fighter instanceof Enemy enemy) return enemy.getLevel();
        return 1;
    }

    void damage(int amount) { shownHp = Math.max(0, shownHp - amount); }
    void heal(int amount) { shownHp = Math.min(maxHp, shownHp + amount); }
    void fall() { shownHp = 0; }
}
