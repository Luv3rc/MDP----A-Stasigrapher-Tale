package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.actors.enemies.Enemy;
import it.unicam.cs.mpgc.rpg130268.combat.battle.FirstUsableAbility;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.EnemyBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.PhasedBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.battle.behaviors.RotationBehavior;
import it.unicam.cs.mpgc.rpg130268.combat.damage.StrikeWeight;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Stats;
import it.unicam.cs.mpgc.rpg130268.combat.mechanics.Weapon;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.Hit;
import it.unicam.cs.mpgc.rpg130268.combat.reaction.ReactionOptions;
import it.unicam.cs.mpgc.rpg130268.combat.skills.TargetType;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAbility;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyAttack;
import it.unicam.cs.mpgc.rpg130268.combat.skills.enemies.EnemyHeal;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static it.unicam.cs.mpgc.rpg130268.game.data.XmlSupport.*;

// What the XML says about one kind of enemy (numbers, weapon, abilities and attack pattern)
// Read once. create() makes a new Enemy every time, ready to fight, and behaviorFor() the pattern
// that goes with that very enemy (patterns are not shared)
final class EnemyDefinition
{
    private sealed interface AbilityDefinition
    {
        String id();
        EnemyAbility createFor(Enemy owner);
    }

    private record AttackDefinition(String id, String name, TargetType target, List<Hit> sequence, String announcement)
            implements AbilityDefinition
    {
        @Override public EnemyAbility createFor(Enemy owner)
        {
            return announced(new EnemyAttack(owner, id, name, target, sequence), announcement);
        }
    }

    private record HealDefinition(String id, String name, TargetType target, double fraction, String announcement)
            implements AbilityDefinition
    {
        @Override public EnemyAbility createFor(Enemy owner)
        {
            return announced(new EnemyHeal(owner, id, name, target, fraction), announcement);
        }
    }

    private static EnemyAbility announced(EnemyAbility ability, String announcement)
    {
        return announcement == null ? ability : ability.withAnnouncement(announcement);
    }

    // The pattern
    // Without a pattern the enemy uses the first ability it can
    private record Phase(double belowFraction, List<String> rotation) { }

    private record Pattern(List<String> rotation, List<Phase> phases) { }

    private final String id;
    private final String name;
    private final String artName;               // null if the pictures have the name that is shown
    private final int level;
    private final int xpReward;
    private final Stats stats;
    private final Weapon weapon;
    private final List<AbilityDefinition> abilities;
    private final Pattern pattern;              // null if there is none

    private EnemyDefinition(String id, String name, String artName, int level, int xpReward, Stats stats, Weapon weapon,
                            List<AbilityDefinition> abilities, Pattern pattern)
    {
        this.id = id;
        this.name = name;
        this.artName = artName;
        this.level = level;
        this.xpReward = xpReward;
        this.stats = stats;
        this.weapon = weapon;
        this.abilities = abilities;
        this.pattern = pattern;

        checkIds();
    }

    String id() { return id; }

    // ---------- Reading ----------

    // Passed the schema
    static EnemyDefinition parse(Element enemy)
    {
        String id = string(enemy, "id");

        try
        {
            Element s = child(enemy, "stats");
            Stats stats = new Stats(integer(s, "health"), integer(s, "strength"), integer(s, "defense"),
                    integer(s, "agility"), integer(s, "luck"));

            List<AbilityDefinition> abilities = children(child(enemy, "abilities")).stream()
                    .map(EnemyDefinition::parseAbility).toList();

            // The weapon is written with the same words as in weapons.xml
            Element weapon = child(enemy, "weapon");

            return new EnemyDefinition(id, string(enemy, "name"), optionalString(enemy, "art"), integer(enemy, "level"), integer(enemy, "xp"), stats,
                    weapon == null ? Weapon.unarmed() : WeaponReader.read(weapon), abilities, parsePattern(child(enemy, "pattern")));
        }
        catch (IllegalArgumentException e)       // a number the game does not accept
        {
            throw new GameDataException("enemy '" + id + "': " + e.getMessage(), e);
        }
    }

    private static AbilityDefinition parseAbility(Element ability)
    {
        String id = string(ability, "id");
        String name = string(ability, "name");
        TargetType target = constant(TargetType.class, ability, "target");
        String announcement = optionalString(ability, "announcement");

        if (ability.getLocalName().equals("heal"))
            return new HealDefinition(id, name, target, decimal(ability, "fraction"), announcement);

        List<Hit> sequence = children(ability, "hit").stream().map(hit -> new Hit(
                longInteger(hit, "windUpMs"),
                constant(ReactionOptions.class, hit, "reaction"),
                constant(StrikeWeight.class, hit, "weight"))).toList();

        return new AttackDefinition(id, name, target, sequence, announcement);
    }

    private static Pattern parsePattern(Element pattern)
    {
        if (pattern == null)
            return null;

        List<Phase> phases = children(pattern, "below").stream()
                .map(below -> new Phase(decimal(below, "hp"), parseRotation(child(below, "rotation")))).toList();

        return new Pattern(parseRotation(child(pattern, "rotation")), phases);
    }

    private static List<String> parseRotation(Element rotation)
    {
        return children(rotation, "use").stream().map(use -> string(use, "ability")).toList();
    }

    // Checks that the abilities have different ids and that the pattern does not use an ability the enemy does not have
    private void checkIds()
    {
        Set<String> known = new HashSet<>();

        for (AbilityDefinition ability : abilities)
            if (!known.add(ability.id()))
                throw new GameDataException("enemy '" + id + "' has two abilities with the id '" + ability.id() + "'");

        if (pattern == null)
            return;

        List<List<String>> rotations = new java.util.ArrayList<>();
        rotations.add(pattern.rotation());
        pattern.phases().forEach(phase -> rotations.add(phase.rotation()));

        for (List<String> rotation : rotations)
            for (String used : rotation)
                if (!known.contains(used))
                    throw new GameDataException("enemy '" + id + "': the pattern uses '" + used + "', which it does not have");
    }

    // ---------- Making ----------

    Enemy create()
    {
        Enemy enemy = new Enemy(name, stats, level, xpReward);
        enemy.equip(weapon);

        if (artName != null)
            enemy.withArtName(artName);

        for (AbilityDefinition ability : abilities)
            enemy.learn(ability::createFor);

        return enemy;
    }

    EnemyBehavior behaviorFor()
    {
        if (pattern == null)
            return new FirstUsableAbility();

        if (pattern.phases().isEmpty())
            return new RotationBehavior(pattern.rotation());

        PhasedBehavior phased = new PhasedBehavior(new RotationBehavior(pattern.rotation()));

        for (Phase phase : pattern.phases())
            phased.below(phase.belowFraction(), new RotationBehavior(phase.rotation()));

        return phased;
    }
}
