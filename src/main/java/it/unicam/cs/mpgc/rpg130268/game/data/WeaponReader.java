package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.Ids;
import it.unicam.cs.mpgc.rpg130268.combat.damage.Scaling;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingGrade;
import it.unicam.cs.mpgc.rpg130268.combat.damage.ScalingStat;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.WeaponType;
import org.w3c.dom.Element;

import static it.unicam.cs.mpgc.rpg130268.game.data.XmlSupport.*;

// A <weapon> element of an XML file, as a Weapon. The same words are used wherever a weapon is written (weapons.xml and,
// later, the enemies), so it is read in one place.
// The weapon scales with strength by its strengthGrade and, if it has them, with one more stat
// (extraStat and extraGrade, together). This is the same as before, just in an XML file
final class WeaponReader
{
    private WeaponReader() { }

    // Without an "id" the weapon takes the one its name gives
    static Weapon read(Element element)
    {
        String name = string(element, "name");
        String id = optionalString(element, "id");

        try
        {
            Scaling scaling = Scaling.of(ScalingStat.STRENGTH, constant(ScalingGrade.class, element, "strengthGrade"));

            String extraStat = optionalString(element, "extraStat");
            String extraGrade = optionalString(element, "extraGrade");

            if ((extraStat == null) != (extraGrade == null))
                throw new GameDataException("weapon  + name + : extraStat and extraGrade go together");

            if (extraStat != null)
                scaling = scaling.and(constant(ScalingStat.class, element, "extraStat"),
                        constant(ScalingGrade.class, element, "extraGrade"));

            return new Weapon(id != null ? id : Ids.slug(name), name, constant(WeaponType.class, element, "type"),
                    integer(element, "damage"), scaling);
        }
        catch (IllegalArgumentException e)       // a number or a combination the game does not accept
        {
            throw new GameDataException("weapon  + name + : " + e.getMessage(), e);
        }
    }
}
