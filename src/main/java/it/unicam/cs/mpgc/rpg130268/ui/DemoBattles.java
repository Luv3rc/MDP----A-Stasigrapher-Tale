package it.unicam.cs.mpgc.rpg130268.ui;

import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.game.EncounterSetup;
import it.unicam.cs.mpgc.rpg130268.game.data.GameData;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

// Small fights to try the battle in a window
public final class DemoBattles
{
    public static final String DEFAULT_ENCOUNTER = "sariel";

    private static final String FLAG = "--battle";

    private static GameData data;

    private DemoBattles() { }

    public static String encounterFrom(List<String> arguments)
    {
        int flag = arguments.indexOf(FLAG);
        int next = flag >= 0 ? flag + 1 : 0;

        return next < arguments.size() && !arguments.get(next).startsWith("--") ? arguments.get(next) : DEFAULT_ENCOUNTER;
    }

    public static BattlePresenter create() { return create(DEFAULT_ENCOUNTER); }

    public static BattlePresenter create(String encounterId)
    {
        return BattlePresenterFactory.create(newParty(), encounter(encounterId));
    }

    // The party of a new game, new members every time
    public static List<PartyMember> newParty()
    {
        GameData data = data();

        return data.members().startingParty().stream().map(data.members()::newMember).toList();
    }

    // The enemies of the encounter, new every time. An id that does not exist says which ones do
    public static EncounterSetup encounter(String encounterId)
    {
        GameData data = data();

        if (!data.encounters().contains(encounterId))
            throw new IllegalArgumentException("there is no encounter \"" + encounterId + "\", try one of " + ids(data.encounters().ids()));

        return data.encounters().encounter(encounterId).spawn();
    }

    private static synchronized GameData data()
    {
        if (data == null)
            data = GameData.bundled();

        return data;
    }

    private static Set<String> ids(Set<String> ids) { return new TreeSet<>(ids); }
}
