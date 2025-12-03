package edu.upc.dmag.signinginterface;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Utilities to parse XML content stored in a String.
 */
public final class XmlParserUtils {

    private XmlParserUtils() { }

    /**
     * Parse the given XML content and return the direct child elements of the root element,
     * excluding any element whose local name (or node name) equals "signatures" (case-insensitive).
     *
     * @param xmlContent XML content as String
     * @return list of Element children (empty list if none or input is null/blank)
     * @throws Exception on parsing errors
     */
    public static List<Element> getRootChildrenExcludingSignatures(String xmlContent) throws Exception {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            return Collections.emptyList();
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        // harden parser against XXE
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        try {
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            // some JREs may not support these attributes; ignore if not available
        }

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xmlContent)));
        Element root = doc.getDocumentElement();
        if (root == null) return Collections.emptyList();

        NodeList children = root.getChildNodes();
        List<Element> result = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) n;
                String name = el.getLocalName() != null ? el.getLocalName() : el.getNodeName();
                if (!"signatures".equalsIgnoreCase(name)) {
                    result.add(el);
                }
            }
        }
        return result;
    }

    /**
     * Convert a DOM Element to a compact XML String. Useful for debugging or logging the extracted children.
     */
    public static String elementToString(Element element) {
        if (element == null) return "";
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return "" + e.getMessage();
        }
    }
}

