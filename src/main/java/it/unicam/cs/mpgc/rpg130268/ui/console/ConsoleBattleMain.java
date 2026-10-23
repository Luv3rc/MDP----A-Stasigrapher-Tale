package it.unicam.cs.mpgc.rpg130268.ui.console;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.combat.battle.BattleClock;
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

        Enemy goblin = new Enemy("Goblin", new Stats(730, 14, 8, 3, 0),
                2, 130);
        goblin.equip(new Weapon("Club", WeaponType.UNARMED, 53, ScalingGrade.NONE));
        goblin.learn(e -> new EnemyAttack(e, "Club swing", TargetType.ONE_ENEMY,
                List.of(new Hit(3000, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL))));

        Enemy bandit = new Enemy("Bandit", new Stats(500, 12, 6, 9, 5),
                3, 200);
        bandit.equip(new Weapon("Dagger", WeaponType.DAGGER, 27, ScalingGrade.NONE, ScalingStat.AGILITY, ScalingGrade.B));
        bandit.learn(e -> new EnemyAttack(e, "Daggers fury", TargetType.ONE_ENEMY,
                List.of(new Hit(1200, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                        new Hit(800, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL),
                        new Hit(800, ReactionOptions.PARRY_OR_DODGE, StrikeWeight.NORMAL))));

        Battle battle = new Battle(List.of(gunther), List.of(goblin, bandit), new BattleClock());
        ConsoleBattle runner = new ConsoleBattle(battle, ReactionWindows.defaults(), new InputLockout(PENALTY_MS), System.out);

        System.out.println("Type the number of your choice. When you see >>> NOW! type e (parry) or q (dodge) and press Enter" +
                "\nBase Attack       -- 1 normal hit. Gives you 1 ink and 1 AP" +
                "\nStasigraphic Fury -- 3 normal hits in a row. 4 Ink minimum. Uses your INK to increase the damage of each hit" +
                "\nCuts for Ink      -- 2 light hits followed by a normal one. For each hit, you gain 1 ink" +
                "\nThirst for Ink    -- Puts you at 1hp, filling your Ink and AP to the max");

        ConsoleBattleLoop.run(runner, System.in);
    }
}