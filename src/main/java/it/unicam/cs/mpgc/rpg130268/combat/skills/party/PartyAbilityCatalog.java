package it.unicam.cs.mpgc.rpg130268.combat.skills.party;

import it.unicam.cs.mpgc.rpg130268.combat.Ids;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.Gunther;
import it.unicam.cs.mpgc.rpg130268.combat.actors.party.PartyMember;
import it.unicam.cs.mpgc.rpg130268.combat.skills.Ability;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.CutsForInk;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.StasigraphicFury;
import it.unicam.cs.mpgc.rpg130268.combat.skills.party.gunther.ThirstForInk;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/* *
    The abilities of the party, by id.
    members.xml and the saves say "cuts-for-ink" and this is where the id becomes
    the ability again, made for the member who learns it.
    Every ability is for one kind of member (the ones that spend ink are for Gunther)
* */
public final class PartyAbilityCatalog
{
    private record Entry<U extends PartyMember>(Class<U> user, Function<U, Ability> maker)
    {
        Ability makeFor(PartyMember member) { return maker.apply(user.cast(member)); }
    }

    private final Map<String, Entry<?>> entries = new LinkedHashMap<>();

    public static PartyAbilityCatalog standard()
    {
        return new PartyAbilityCatalog()
                .register(BasicAttack.ID, PartyMember.class, BasicAttack::new)
                .register(CutsForInk.ID, Gunther.class, CutsForInk::new)
                .register(StasigraphicFury.ID, Gunther.class, StasigraphicFury::new)
                .register(ThirstForInk.ID, Gunther.class, ThirstForInk::new);
    }

    public <U extends PartyMember> PartyAbilityCatalog register(String id, Class<U> user, Function<U, Ability> maker)
    {
        Ids.require(id);
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(maker, "maker must not be null");

        if (entries.putIfAbsent(id, new Entry<>(user, maker)) != null)
            throw new IllegalArgumentException("the ability \"" + id + "\" is already registered");

        return this;
    }

    public boolean contains(String id) { return entries.containsKey(id); }

    public Set<String> ids() { return Set.copyOf(entries.keySet()); }

    public Ability createFor(String id, PartyMember member)
    {
        Objects.requireNonNull(member, "member must not be null");

        Entry<?> entry = entries.get(id);

        if (entry == null)
            throw new IllegalArgumentException("there is no ability \"" + id + "\"");

        if (!entry.user().isInstance(member))
            throw new IllegalArgumentException(member.getName() + " cannot use \"" + id + "\": it is for "
                    + entry.user().getSimpleName());

        return entry.makeFor(member);
    }
}
