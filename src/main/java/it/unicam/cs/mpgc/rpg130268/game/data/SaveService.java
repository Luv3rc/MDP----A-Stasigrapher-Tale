package it.unicam.cs.mpgc.rpg130268.game.data;

import it.unicam.cs.mpgc.rpg130268.combat.mechanics.StatPoints;
import it.unicam.cs.mpgc.rpg130268.game.GameFlow;
import it.unicam.cs.mpgc.rpg130268.game.GameSnapshot;
import it.unicam.cs.mpgc.rpg130268.game.MemberState;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

import static it.unicam.cs.mpgc.rpg130268.game.data.XmlSupport.*;

/* *
    Saves and loads a GameSnapshot as an XML file, for example

    <save version="2">
      <position step="forest" trekProgress="2"/>
      <party>
        <member id="gunther" level="2" xp="30" hp="150" unspentPoints="3" weapon="thesmosia">
          <points health="0" strength="2" defense="0" agility="1" luck="0"/>
          <abilities><learn ref="basic-attack"/><learn ref="cuts-for-ink"/></abilities>
        </member>
      </party>
    </save>

    The version is the format of the file. A file with another version is refused, before
    anything else is looked at, because an older format does not even have the same elements
    The file is checked against the schema when it is read, so a file that was edited by hand or damaged is refused
    and never gives a half made game. When writing, the file is made first beside the real one
    and then moved in its place, so a crash while saving does not destroy the old save
* */
public final class SaveService
{
    // The format of the files this version of the game writes and reads. It goes up when the format changes
    public static final int FORMAT_VERSION = 2;

    private static final String SCHEMA = "/game/save.xsd";

    private SaveService() { }

    public static boolean exists(Path file) { return Files.isRegularFile(file); }

    public static void write(GameSnapshot snapshot, Path file)
    {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(file, "file must not be null");

        try
        {
            Path folder = file.toAbsolutePath().getParent();
            Files.createDirectories(folder);

            Path temporary = Files.createTempFile(folder, "save", ".tmp");

            try
            {
                // the stream is closed before the move
                try (OutputStream out = Files.newOutputStream(temporary))
                {
                    transformer().transform(new DOMSource(toDocument(snapshot)), new StreamResult(out));
                }

                replace(temporary, file);
            }
            catch (TransformerException | IOException | ParserConfigurationException | RuntimeException e)
            {
                Files.deleteIfExists(temporary);
                throw e;
            }
        }
        catch (IOException | TransformerException | ParserConfigurationException e)
        {
            throw new GameDataException("can't write the save " + file + ": " + e.getMessage(), e);
        }
    }

    public static GameSnapshot read(Path file)
    {
        Objects.requireNonNull(file, "file must not be null");

        try
        {
            byte[] content = Files.readAllBytes(file);

            checkVersion(content, file);

            Element root = parse(new ByteArrayInputStream(content), file.toString(), SCHEMA).getDocumentElement();
            Element position = child(root, "position");

            List<MemberState> party = children(child(root, "party"), "member").stream()
                    .map(SaveService::readMember).toList();

            return new GameSnapshot(string(position, "step"), integer(position, "trekProgress"), party);
        }
        catch (IOException e)
        {
            throw new GameDataException("can't read the save " + file + ": " + e.getMessage(), e);
        }
        catch (IllegalArgumentException e)
        {
            throw new GameDataException("the save " + file + " does not make sense: " + e.getMessage(), e);
        }
    }

    public static void load(Path file, GameFlow flow)
    {
        Objects.requireNonNull(flow, "flow must not be null");

        GameSnapshot snapshot = read(file);

        try
        {
            flow.restore(snapshot);
        }
        catch (IllegalArgumentException | GameDataException e)
        {
            throw new GameDataException("the save " + file + " does not fit this game: " + e.getMessage(), e);
        }
    }


    // ---------- Inside ----------

    private static void checkVersion(byte[] content, Path file)
    {
        Element root = parseLoosely(new ByteArrayInputStream(content), file.toString()).getDocumentElement();

        if (!root.getTagName().equals("save"))
            return;

        try
        {
            int version = Integer.parseInt(root.getAttribute("version").trim());

            if (version != FORMAT_VERSION)
                throw new GameDataException("the save " + file + " was made with another version of the game (its format is "
                        + version + " and this game reads format " + FORMAT_VERSION + ")");
        }
        catch (NumberFormatException e)
        {
            // a missing or strange version is refused by the schema, with its own message
        }
    }

    private static MemberState readMember(Element member)
    {
        Element points = child(member, "points");

        List<String> abilities = children(child(member, "abilities"), "learn").stream()
                .map(learn -> string(learn, "ref")).toList();

        return new MemberState(string(member, "id"), integer(member, "level"), integer(member, "xp"), integer(member, "hp"),
                new StatPoints(integer(points, "health"), integer(points, "strength"), integer(points, "defense"),
                        integer(points, "agility"), integer(points, "luck")),
                integer(member, "unspentPoints"), string(member, "weapon"), abilities);
    }

    private static Document toDocument(GameSnapshot snapshot) throws ParserConfigurationException
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        Element save = document.createElement("save");
        save.setAttribute("version", String.valueOf(FORMAT_VERSION));
        document.appendChild(save);

        Element position = document.createElement("position");
        position.setAttribute("step", snapshot.stepId());
        position.setAttribute("trekProgress", String.valueOf(snapshot.trekProgress()));
        save.appendChild(position);

        Element party = document.createElement("party");
        save.appendChild(party);

        for (MemberState state : snapshot.party())
            party.appendChild(toElement(document, state));

        return document;
    }

    private static Element toElement(Document document, MemberState state)
    {
        Element member = document.createElement("member");
        member.setAttribute("id", state.id());
        member.setAttribute("level", String.valueOf(state.level()));
        member.setAttribute("xp", String.valueOf(state.xp()));
        member.setAttribute("hp", String.valueOf(state.hp()));
        member.setAttribute("unspentPoints", String.valueOf(state.unspentPoints()));
        member.setAttribute("weapon", state.weaponId());

        Element points = document.createElement("points");
        points.setAttribute("health", String.valueOf(state.points().health()));
        points.setAttribute("strength", String.valueOf(state.points().strength()));
        points.setAttribute("defense", String.valueOf(state.points().defense()));
        points.setAttribute("agility", String.valueOf(state.points().agility()));
        points.setAttribute("luck", String.valueOf(state.points().luck()));
        member.appendChild(points);

        Element abilities = document.createElement("abilities");
        for (String id : state.abilityIds())
        {
            Element learn = document.createElement("learn");
            learn.setAttribute("ref", id);
            abilities.appendChild(learn);
        }
        member.appendChild(abilities);

        return member;
    }

    // Moves the new file over the old one in one step when the system can do it, so there is never a moment with
    // half a save in place
    private static void replace(Path temporary, Path file) throws IOException
    {
        try
        {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Transformer transformer() throws TransformerException
    {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        return transformer;
    }
}
