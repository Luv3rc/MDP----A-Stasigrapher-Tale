package it.unicam.cs.mpgc.rpg130268.ui.fx;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/* *
    Where the pictures of the battle are looked for: src/main/resources/sprites/<name>.png
    A picture that is not there is not an error: null (or the plain look), so the game works without any art
    and every picture can be added one at a time.

    The fighters (name = the name of the fighter, in lowercase, every other character becomes "_"):
        <name>_back.png    a party member seen from behind, on the field       gunther_back.png
        <name>_front.png   an enemy seen from the front, on the field          mattias27_front.png
        <name>_head.png    the face of a party member, in the HUD              gunther_head.png
    The screen:
        background_<area>.png   the background of the battles that take place in that area, e.g. background_forest.png
                                (16:9, it is stretched to the whole window)
        background.png          the background of every area that has no picture of its own
    (the buttons are not pictures: they are drawn by css/battle.css)
* */
final class SpriteArt
{
    private static final Map<String, Image> CACHE = new HashMap<>();

    private SpriteArt() { }

    // The picture of a fighter: <name>_<kind>.png
    static Image find(String fighterName, String kind)
    {
        String key = fighterName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");

        return ui(key + "_" + kind);
    }

    // A picture of the screen: <name>.png (null if there is none)
    static Image ui(String name)
    {
        return CACHE.computeIfAbsent(name, key ->
        {
            URL url = urlOf(key);
            return url == null ? null : new Image(url.toExternalForm());
        });
    }

    // Where the picture is, for the style sheets (null if there is none)
    static String url(String name)
    {
        URL url = urlOf(name);
        return url == null ? null : url.toExternalForm();
    }

    private static URL urlOf(String name)
    {
        return SpriteArt.class.getResource("/sprites/" + name + ".png");
    }
}