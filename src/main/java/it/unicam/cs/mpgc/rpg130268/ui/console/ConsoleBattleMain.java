package it.unicam.cs.mpgc.rpg130268.ui.console;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.battle.Battle;
import it.unicam.cs.mpgc.rpg130268.game.BattleFactory;
import it.unicam.cs.mpgc.rpg130268.game.EncounterSetup;
import it.unicam.cs.mpgc.rpg130268.ui.DemoBattles;
import it.unicam.cs.mpgc.rpg130268.ui.InputLockout;

import java.io.IOException;
import java.util.List;

// A small fight to try the battle in the console: Gunther against a goblin
public class ConsoleBattleMain
{
    private static final long PENALTY_MS = 500;   // how long the keys are locked after a key that counts for nothing

    public static void main(String[] args) throws InterruptedException, IOException
    {
        List<PartyMember> party = DemoBattles.newParty();
        EncounterSetup setup = DemoBattles.encounter(DemoBattles.encounterFrom(List.of(args)));

        Battle battle = BattleFactory.create(party, setup);
        ConsoleBattle runner = new ConsoleBattle(battle, BattleFactory.reactionWindows(), new InputLockout(PENALTY_MS), System.out);

        System.out.println("Type the number of your choice. When you see >>> NOW! type e (parry) or q (dodge) and press Enter" +
                "\nBase Attack       -- 1 normal hit. Gives you 1 ink and 1 AP" +
                "\nStasigraphic Fury -- 3 normal hits in a row. 4 Ink minimum. Uses your INK to increase the damage of each hit" +
                "\nCuts for Ink      -- 2 light hits followed by a normal one. For each hit, you gain 1 ink" +
                "\nThirst for Ink    -- Puts you at 1hp, filling your Ink and AP to the max");

        ConsoleBattleLoop.run(runner, System.in);
    }
}
