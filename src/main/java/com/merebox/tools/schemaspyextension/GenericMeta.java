/**
 * 
 */
package com.merebox.tools.schemaspyextension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.schemaspy.Config;
import org.schemaspy.model.InvalidConfigurationException;
import org.schemaspy.model.xml.ModelExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author tomp
 *
 */
public class GenericMeta implements ModelExtension {

	private static String SCHEMA_XSD = "schemaspy.meta.xsd";

	// private List<TableMeta> tables = new ArrayList<TableMeta>();
	private String comments;
	private File metaFile;
	private InputStream xslStream;
	private final Logger logger = Logger.getLogger(getClass().getName());

	private void validate(Document document) throws SAXException, IOException {
		// create a SchemaFactory capable of understanding WXS schemas
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		// load a WXS schema, represented by a Schema instance
		Schema schema = factory.newSchema(new StreamSource(xslStream));

		// create a Validator instance, which can be used to validate an
		// instance document
		Validator validator = schema.newValidator();

		// validate the DOM tree
		validator.validate(new DOMSource(document));
	}

	private Document parse(File file) throws InvalidConfigurationException {
		DocumentBuilder docBuilder;
		Document doc = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);

		try {
			docBuilder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException exc) {
			throw new InvalidConfigurationException("Invalid XML parser configuration", exc);
		}

		try {
			logger.info("Parsing " + file);
			doc = docBuilder.parse(file);
		} catch (SAXException exc) {
			throw new InvalidConfigurationException("Failed to parse " + file, exc);
		} catch (IOException exc) {
			throw new InvalidConfigurationException("Could not read " + file + ":", exc);
		}
		try {
			validate(doc);
		} catch (SAXException exc) {
			logger.warning("Failed to validate " + file + ": " + exc);
		} catch (IOException exc) {
			logger.warning("Failed to validate " + file + ": " + exc);
		}

		return doc;
	}

	public void loadModelExtension(String xmlMeta, String dbName, String schema) throws InvalidConfigurationException {

		if (xmlMeta != null && xmlMeta.trim().length() > 0) {
			File meta = new File(xmlMeta);
			if (meta.isDirectory()) {
				String filename = (schema == null ? dbName : schema) + ".meta.xml";
				meta = new File(meta, filename);

				if (!meta.exists()) {
					if (Config.getInstance().isOneOfMultipleSchemas()) {
						// don't force all of the "one of many" schemas to have
						// metafiles
						logger.info(
								"Meta directory \"" + xmlMeta + "\" should contain a file named \"" + filename + '\"');
						comments = null;
						metaFile = null;
						return;
					}

					throw new InvalidConfigurationException(
							"Meta directory \"" + xmlMeta + "\" must contain a file named \"" + filename + '\"');
				}
			} else if (!meta.exists()) {
				throw new InvalidConfigurationException("Specified meta file \"" + xmlMeta + "\" does not exist");
			}

			metaFile = meta;

			// Now try and locate the Meta XSD
			File xsd = new File(metaFile.getParent() + File.separator + SCHEMA_XSD);
			if (xsd.exists()) {
				try {
					xslStream = new FileInputStream(xsd);
				} catch (FileNotFoundException fnfe) {
					throw new InvalidConfigurationException(
							"Schema XSD file (" + xsd.getAbsolutePath() + ") failed to load");
				}
			} else {
				// Note that this in this JARs resource if no file found
				xslStream = getClass().getResourceAsStream("/" + SCHEMA_XSD);
			}

			Document doc = parse(metaFile);

			NodeList commentsNodes = doc.getElementsByTagName("comments");
			if (commentsNodes != null && commentsNodes.getLength() > 0)
				comments = commentsNodes.item(0).getTextContent();
			else
				comments = null;

			NodeList tablesNodes = doc.getElementsByTagName("tables");
			if (tablesNodes != null) {
				NodeList tableNodes = ((Element) tablesNodes.item(0)).getElementsByTagName("table");

				for (int i = 0; i < tableNodes.getLength(); ++i) {
					Node tableNode = tableNodes.item(i);
				}
			}
		} else {

			// Load the shell of the tables
			ArrayList<String> ts = new ArrayList<String>();
			ts.add("ReportLayoutMatrix_Epic5.1AC01");
			ts.add("ReportLayoutMatrix_Epic5.1AC02");
			ts.add("ReportLayoutMatrix_Epic5.1AC03");
		}
	}

	public String getValue(String table, String column, String key) {

		if (table == null && column == null && key != null && key.compareTo("comments") == 0)
			return "Default extension comment for database";

		if (table != null && column == null && key != null && key.compareTo("comments") == 0)
			return "Default extension comment for a table";

		if (table != null && column != null && key != null && key.compareTo("comments") == 0)
			return "Default extension comment for a column";

		return null;
	}

	public Map<String, String> getValues(String table, String column) {

		return null;
	}

	public String getHeader(String table, String column, String key) {

		if (table == null && column == null && key != null && key.compareTo("comments") == 0)
			return "Comment";

		return null;
	}

	public Map<String, String> getHeaders(String table, String column) {
		return null;
	}

	public String getLeftLabel(String table, String column, String key) {
		if (table == null && column == null && key != null && key.compareTo("comments") == 0)
			return "Comment";

		return null;
	}

	public Map<String, String> getLeftLabels(String table, String column) {

		return null;
	}

	public String getRightLabel(String table, String column, String key) {

		return null;
	}

	public Map<String, String> getRightLabels(String table, String column) {

		return null;
	}

}
