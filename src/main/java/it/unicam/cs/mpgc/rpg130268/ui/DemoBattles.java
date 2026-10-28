package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.*;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.EnemyBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.PerEnemyBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.PhasedBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.RotationBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.damage.DamageCalculator;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ap.ApRules;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.ink.InkRules;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionWindows;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAttack;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyHeal;

import java.util.List;

// Small fights to try the battle in a window
public final class DemoBattles
{
    private static final long PENALTY_MS = 400;   // how long the keys are locked after a key that counts for nothing
    private static final long DEAD_ZONE_MS = 1200;     // the pause between the end of an action and the start of the next one
    private static final long ANNOUNCEMENT_MS = 1600;  // how long the text of an ability that announces itself stays



    private DemoBattles() { }

    public static BattlePresenter create()
    {
        Gunther gunther = new Gunther(new Stats(184, 8, 4, 10, 3),
                9, 3, ApRules.defaults(), InkRules.defaults());
        gunther.equip(new Weapon("Thesmosia", WeaponType.SWORD, 32, ScalingGrade.NONE,
                ScalingStat.AGILITY, ScalingGrade.D));
        /*
        // A pattern the player can learn: a slow swing (two hits, the second one can only be parried),
        // then a quick jab, and again
        Enemy goblin = new Enemy("Goblin", new Stats(40, 10, 2, 3, 0), 2, 30);
        goblin.equip(new Weapon("Club", WeaponType.UNARMED, 10, ScalingGrade.NONE));
        goblin.learn(e -> new EnemyAttack(e, "Club swing", TargetType.ONE_ENEMY, List.of(
                new Hit(900, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                new Hit(600, ReactionOptions.PARRY_ONLY, StrikeWeight.NORMAL))));
        goblin.learn(e -> new EnemyAttack(e, "Quick jab", TargetType.ONE_ENEMY, List.of(
                new Hit(500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT))));

        // A small boss: a slow and heavy smash, and when it is down to half its health it gets angry
        // and adds a frenzy of three quick hits
        Enemy brute = new Enemy("Brute", new Stats(60, 12, 3, 2, 0), 3, 50);
        brute.equip(new Weapon("Maul", WeaponType.UNARMED, 12, ScalingGrade.NONE));
        brute.learn(e -> new EnemyAttack(e, "Smash", TargetType.ONE_ENEMY, List.of(
                new Hit(1300, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.HEAVY))));
        brute.learn(e -> new EnemyAttack(e, "Frenzy", TargetType.ONE_ENEMY, List.of(
                new Hit(600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                new Hit(450, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                new Hit(450, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT))));

        EnemyBehavior patterns = new PerEnemyBehavior(new FirstUsableAbility())
                .assign(goblin, new RotationBehavior(List.of("Club swing", "Quick jab")))
                .assign(brute, new PhasedBehavior(new RotationBehavior(List.of("Smash")))
                        .below(0.5, new RotationBehavior(List.of("Smash", "Frenzy"))));
        */

        Enemy Mattias27 = new Enemy("Mattias27", new Stats(1400, 8, 2, 10, 0),
                4, 800);
        Mattias27.equip(new Weapon("Shortsword", WeaponType.SWORD, 36, ScalingGrade.NONE, ScalingStat.AGILITY, ScalingGrade.B));
        Mattias27.learn(e -> new EnemyAttack(e, "Tagli a croce", TargetType.ONE_ENEMY,
                List.of(new Hit(1500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                        new Hit(600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                        new Hit (800, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                        new Hit (600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT))));
        Mattias27.learn(e -> new EnemyAttack(e, "Taglio del Rumeno", TargetType.ONE_ENEMY,
                List.of(new Hit(2000, ReactionOptions.PARRY_ONLY, StrikeWeight.HEAVY),
                        new Hit(700, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL))));
        Mattias27.learn(e -> new EnemyHeal(e, "Heal Allay", TargetType.ONE_ALLY, 0.3).
                withAnnouncement("{user} heals {target}!"));

        Enemy Alex = new Enemy("Alex", new Stats(800, 12, 10, 4, 6),
                3, 500);
        Alex.equip(new Weapon("Greatsword", WeaponType.SWORD, 67, ScalingGrade.NONE, ScalingStat.STRENGTH, ScalingGrade.C));
        Alex.learn(e -> new EnemyAttack(e, "Attacco Pendel", TargetType.ONE_ENEMY,
                List.of(new Hit(2000, ReactionOptions.PARRY_ONLY, StrikeWeight.HEAVY),
                        new Hit(700, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL))));
        Alex.learn(e -> new EnemyAttack(e, "Puzza della Legge con Tagli", TargetType.ONE_ENEMY,
                List.of(new Hit(1300, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.HEAVY),
                        new Hit(600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                        new Hit(800, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL)))
                .withAnnouncement("{user} attacks with Puzza della Legge!"));
        Alex.learn(e -> new EnemyAttack(e, "Furia Legislativa", TargetType.ONE_ENEMY,
                List.of(new Hit(2000, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.HEAVY),
                        new Hit(600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                        new Hit(800, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                        new Hit(600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                        new Hit(800, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                        new Hit(1400, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.HEAVY)))
                .withAnnouncement("{user} attacks with Furia Legislativa!"));

        List<PartyMember> party = List.of(gunther);
        List<Enemy> enemies = List.of(Mattias27, Alex);

        EnemyBehavior patterns = new PerEnemyBehavior(new FirstUsableAbility())
                .assign(Mattias27, new RotationBehavior(List.of("Tagli a croce", "Taglio del Rumeno", "Heal Allay")))
                .assign(Alex, new PhasedBehavior(new RotationBehavior(List.of("Attacco Pendel")))
                        .below(0.5, new RotationBehavior(List.of("Puzza della Legge con Tagli", "Furia Legislativa"))));

        BattleRules rules = new BattleRules(StrikeWeight.HEAVY, DEAD_ZONE_MS, ANNOUNCEMENT_MS);
        LiveBattleContext context = new LiveBattleContext(new DamageCalculator(), ReactionWindows.defaults(),
                rules, new BattleClock());
        Battle battle = new Battle(party, enemies, context, patterns);     // with the rules above: dead zone, announcements
        BattlePresenter presenter = new BattlePresenter(battle, party, enemies, ReactionWindows.defaults(),
                new InputLockout(PENALTY_MS));
        presenter.setArea("arena");     // the background: background_arena.png (or background.png if there is none)

        return presenter;
    }
}