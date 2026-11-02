package it.unicam.cs.mpgc.rpg130268.combat;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/* *
    The ids of the things that the outside of the code points to (abilities, weapons, and later members, items...)
    Saves and XML files say "cuts-for-ink", never "Cuts for Ink". The id is STABLE, it never changes, while the
    name is only what the player reads and can be rewritten or translated without breaking anything.
    An id is made of lowercase words joined by "-": "cuts-for-ink", "club-swing", "quick-jab"
* */
public final class Ids
{
    private static final Pattern VALID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern NOT_ALLOWED = Pattern.compile("[^a-z0-9]+");
    private static final Pattern ACCENTS = Pattern.compile("\\p{M}+");

    private Ids() { }

    // The same id, if it is well formed
    public static String require(String id)
    {
        Objects.requireNonNull(id, "id must not be null");

        if (!VALID.matcher(id).matches())
            throw new IllegalArgumentException("\"" + id + "\" is not a valid id (lowercase words joined by \"-\", like \"club-swing\")");

        return id;
    }

    // The id that a name gives when nobody chooses one: "Club swing" -> "club-swing", "Tagli a croce" -> "tagli-a-croce"
    // It is a convenience for what is built in code (and in tests); what is saved or written in a file gets its id on purpose
    public static String slug(String name)
    {
        Objects.requireNonNull(name, "name must not be null");

        String plain = ACCENTS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");
        String id = NOT_ALLOWED.matcher(plain.toLowerCase(Locale.ROOT)).replaceAll("-");

        id = id.replaceAll("^-+|-+$", "");

        if (id.isEmpty())
            throw new IllegalArgumentException("no id can be made from the name \"" + name + "\"");

        return id;
    }
}
