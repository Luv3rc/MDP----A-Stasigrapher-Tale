package it.unicam.cs.mpgc.rpg130268.ui.console;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleClock;
import it.unicam.cs.mpgc.rpg130268.combat.battle.FirstUsableAbility;
import it.unicam.cs.mpgc.rpg130268.combat.battle.LiveBattleContext;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.EnemyBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.PerEnemyBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.PhasedBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.RotationBehavior;
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
import it.unicam.cs.mpgc.rpg130268.ui.InputLockout;

import java.io.IOException;
import java.util.List;

// A small fight to try the battle in the console: Gunther against a goblin
public class ConsoleBattleMain
{
    private static final long PENALTY_MS = 500;   // how long the keys are locked after a key that counts for nothing

    public static void main(String[] args) throws InterruptedException, IOException {
        Gunther gunther = new Gunther(new Stats(164, 8, 4, 12, 3),
                9, 3, ApRules.defaults(), InkRules.defaults());
        gunther.equip(new Weapon("Training sword", WeaponType.SWORD, 32, ScalingGrade.NONE,
                ScalingStat.AGILITY, ScalingGrade.C));

        Enemy Mattias27 = new Enemy("Mattias27", new Stats(1400, 8, 2, 10, 0),
                4, 800);
        Mattias27.equip(new Weapon("Shortsword", WeaponType.SWORD, 36, ScalingGrade.NONE,
                ScalingStat.AGILITY, ScalingGrade.B));
        Mattias27.learn(e -> new EnemyAttack(e, "Tagli a croce", TargetType.ONE_ENEMY,
                List.of(new Hit(1500, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                        new Hit(600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                        new Hit (800, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                        new Hit (600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT))));
        Mattias27.learn(e -> new EnemyAttack(e, "Taglio del Rumeno", TargetType.ONE_ENEMY,
                List.of(new Hit(2000, ReactionOptions.PARRY_ONLY, StrikeWeight.HEAVY),
                        new Hit(700, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL))));
        Mattias27.learn(e -> new EnemyHeal(e, "Heal Allay", TargetType.ONE_ALLY, 0.3));

        Enemy Alex = new Enemy("Alex", new Stats(800, 12, 10, 4, 6),
                3, 500);
        Alex.equip(new Weapon("Greatsword", WeaponType.SWORD, 67, ScalingGrade.NONE, ScalingStat.STRENGTH, ScalingGrade.C));
        Alex.learn(e -> new EnemyAttack(e, "Attacco Pendel", TargetType.ONE_ENEMY,
                List.of(new Hit(2000, ReactionOptions.PARRY_ONLY, StrikeWeight.HEAVY),
                        new Hit(700, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL))));
        Alex.learn(e -> new EnemyAttack(e, "Puzza della Legge con Tagli", TargetType.ONE_ENEMY,
                List.of(new Hit(1300, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.HEAVY),
                        new Hit(600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                        new Hit(800, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL))));
        Alex.learn(e -> new EnemyAttack(e, "Furia Legislativa", TargetType.ONE_ENEMY,
                List.of(new Hit(2000, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.HEAVY),
                        new Hit(600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                        new Hit(800, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                        new Hit(600, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.LIGHT),
                        new Hit(800, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                        new Hit(1400, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.HEAVY))));

        List<PartyMember> party = List.of(gunther);
        List<Enemy> enemies = List.of(Mattias27, Alex);

        EnemyBehavior patterns = new PerEnemyBehavior(new FirstUsableAbility())
                .assign(Mattias27, new RotationBehavior(List.of("Tagli a croce", "Taglio del Rumeno", "Heal Allay")))
                .assign(Alex, new PhasedBehavior(new RotationBehavior(List.of("Attacco Pendel")))
                        .below(0.5, new RotationBehavior(List.of("Puzza della Legge con Tagli", "Furia Legislativa"))));

        Battle battle = new Battle(party, enemies, new LiveBattleContext(new BattleClock()), patterns);

        ConsoleBattle runner = new ConsoleBattle(battle, ReactionWindows.defaults(), new InputLockout(PENALTY_MS), System.out);

        System.out.println("Type the number of your choice. When you see >>> NOW! type e (parry) or q (dodge) and press Enter" +
                "\nBase Attack       -- 1 normal hit. Gives you 1 ink and 1 AP" +
                "\nStasigraphic Fury -- 3 normal hits in a row. 4 Ink minimum. Uses your INK to increase the damage of each hit" +
                "\nCuts for Ink      -- 2 light hits followed by a normal one. For each hit, you gain 1 ink" +
                "\nThirst for Ink    -- Puts you at 1hp, filling your Ink and AP to the max");

        ConsoleBattleLoop.run(runner, System.in);
    }
}