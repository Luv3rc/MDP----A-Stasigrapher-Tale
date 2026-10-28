package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/* *
    Where the pictures of the battle are looked for, in src/main/resources/sprites/. A picture that is not there is not
    an error: null (or the plain look), so the game works without any art and every picture can be added one at a time.

    One folder for every character, and one for the backgrounds (the name of a character is in lowercase, every other
    character becomes "_"):

        sprites/characters/gunther/idle.png      standing, in a loop: a strip of square frames, looking to the right
                                   attack.png    when it strikes             hit.png     when it is hit
                                   dodge.png     when it dodges              parry.png   when it parries
                                   defeat.png    when it is defeated (the last frame stays)
                                   head.png      its face, in the HUD (a square)
                                   back.png      (if there is no idle) a still picture of a party member, on the field
                                   front.png     (if there is no idle) a still picture of an enemy, on the field
        sprites/characters/mattias27/idle.png ...
        sprites/backgrounds/arena.png            the background of the battles in the area "arena" (16:9)
        sprites/backgrounds/default.png          the one of every area that has no picture of its own

    The old names still work, with everything in the same folder (gunther_idle.png, background_arena.png,
    background.png): the folders are looked in first
* */
final class SpriteArt
{
    private static final String ROOT = "/sprites/";
    private static final Map<String, Image> CACHE = new HashMap<>();

    private SpriteArt() { }

    // The picture of a fighter: characters/<name>/<kind>.png, or <name>_<kind>.png
    static Image find(String fighterName, String kind)
    {
        String key = key(fighterName);

        Image inFolder = ui("characters/" + key + "/" + kind);
        return inFolder != null ? inFolder : ui(key + "_" + kind);
    }

    // The animation of a fighter, as a strip of frames (null if there is none)
    static SpriteSheet sheet(String fighterName, String animation)
    {
        Image strip = find(fighterName, animation);
        return strip == null ? null : SpriteSheet.of(strip);
    }

    // The background of an area: the picture of the area, else the default one, else the general one, else null
    static Image background(String area)
    {
        String key = key(area);

        for (String candidate : List.of("backgrounds/" + key, "background_" + key, "backgrounds/default", "background"))
        {
            Image picture = ui(candidate);
            if (picture != null) return picture;
        }

        return null;
    }

    // A picture by its path under sprites/, without ".png" (null if there is none)
    static Image ui(String path)
    {
        return CACHE.computeIfAbsent(path, key ->
        {
            URL url = urlOf(key);
            return url == null ? null : new Image(url.toExternalForm());
        });
    }

    // Where the picture is, for the style sheets (null if there is none)
    static String url(String path)
    {
        URL url = urlOf(path);
        return url == null ? null : url.toExternalForm();
    }

    private static String key(String name)
    {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    private static URL urlOf(String path)
    {
        return SpriteArt.class.getResource(ROOT + path + ".png");
    }
}