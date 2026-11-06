package it.unicam.cs.mpgc.rpg130268.game.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/* *
    Common XML utility functions for game data loaders.
    Centralizes DOM parsing with XSD schema validation and provides type-safe
    extraction of element attributes and children. Because structural correctness
    is enforced by the XSD schema at parse time, caller loaders only need to handle
    semantic validation (such as cross-file references and domain constraints)
*/
final class XmlSupport
{
    private XmlSupport() { }

    // ---------- Reading ----------

    static InputStream openResource(String path)
    {
        InputStream in = XmlSupport.class.getResourceAsStream(path);

        if (in == null)
            throw new GameDataException("the resource " + path + " is missing");

        return in;
    }

    // Reads the document and checks it with the schema (a resource, e.g. "/game/story.xsd").
    // "source" is, here too, the name used in the error messages
    static Document parse(InputStream in, String source, String schemaResource)
    {
        try (in)
        {
            URL schemaUrl = XmlSupport.class.getResource(schemaResource);

            if (schemaUrl == null)
                throw new GameDataException("the schema " + schemaResource + " is missing");

            Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaUrl);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setSchema(schema);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);   // no DOCTYPE: no external entities

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new StrictErrorHandler());

            return builder.parse(in);
        }
        catch (SAXParseException e)
        {
            throw new GameDataException(source + ", line " + e.getLineNumber() + ": " + e.getMessage(), e);
        }
        catch (SAXException | IOException | ParserConfigurationException e)
        {
            throw new GameDataException("can't read " + source + ": " + e.getMessage(), e);
        }
    }

    // Reads a document without checking it against any schema. It is only meant to look at a few things before the
    // real reading, like the version of a save, so it never gives the document to anyone else
    static Document parseLoosely(InputStream in, String source)
    {
        try (in)
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new StrictErrorHandler());

            return builder.parse(in);
        }
        catch (SAXParseException e)
        {
            throw new GameDataException(source + ", line " + e.getLineNumber() + ": " + e.getMessage(), e);
        }
        catch (SAXException | IOException | ParserConfigurationException e)
        {
            throw new GameDataException("can't read " + source + ": " + e.getMessage(), e);
        }
    }

    // Warnings are ignored, any error stops the reading
    private static final class StrictErrorHandler implements ErrorHandler
    {
        @Override public void warning(SAXParseException e) { }
        @Override public void error(SAXParseException e) throws SAXException { throw e; }
        @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
    }

    // ---------- Looking inside the elements ----------

    // The child elements (all of them, or only those with this name), in order
    static List<Element> children(Element parent)
    {
        List<Element> result = new ArrayList<>();

        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element element)
                result.add(element);

        return result;
    }

    static List<Element> children(Element parent, String name)
    {
        return children(parent).stream().filter(element -> element.getLocalName().equals(name)).toList();
    }

    // The first child with this name, or null
    static Element child(Element parent, String name)
    {
        List<Element> found = children(parent, name);
        return found.isEmpty() ? null : found.getFirst();
    }

    static String text(Element element) { return element.getTextContent().strip(); }

    static String string(Element element, String attribute) { return element.getAttribute(attribute); }

    // The attribute, or null if it is not there
    static String optionalString(Element element, String attribute)
    {
        return element.hasAttribute(attribute) ? element.getAttribute(attribute) : null;
    }

    static int integer(Element element, String attribute)
    {
        try { return Integer.parseInt(string(element, attribute)); }
        catch (NumberFormatException e) { throw bad(element, attribute, "is not a whole number"); }
    }

    static long longInteger(Element element, String attribute)
    {
        try { return Long.parseLong(string(element, attribute)); }
        catch (NumberFormatException e) { throw bad(element, attribute, "is not a whole number"); }
    }

    static double decimal(Element element, String attribute)
    {
        try { return Double.parseDouble(string(element, attribute)); }
        catch (NumberFormatException e) { throw bad(element, attribute, "is not a number"); }
    }

    static <E extends Enum<E>> E constant(Class<E> type, Element element, String attribute)
    {
        try { return Enum.valueOf(type, string(element, attribute)); }
        catch (IllegalArgumentException e) { throw bad(element, attribute, "is not a valid " + type.getSimpleName()); }
    }

    private static GameDataException bad(Element element, String attribute, String problem)
    {
        return new GameDataException("<" + element.getLocalName() + "> " + attribute + " "
                + problem + ": '" + element.getAttribute(attribute) + "'");
    }
}
