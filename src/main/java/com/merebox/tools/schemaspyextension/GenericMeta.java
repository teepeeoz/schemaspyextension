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
import java.util.HashMap;
import java.util.List;
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
import org.schemaspy.model.xml.MetaModelKeywords;
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

	private final static String LEFT_LABEL = "leftLabel";
	private final static String RIGHT_LABEL = "rightLabel";
	private final static String HEADING = "heading";

	private List<TableMetadata> tables = new ArrayList<TableMetadata>();
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
	                TableMetadata tableMeta = new TableMetadata(tableNode);
	                tables.add(tableMeta);
				}
			}
		} else {

			// Load the shell of the tables
			ArrayList<String> ts = new ArrayList<String>();
		}
	}

	public String getValue(String table, String column, String key) {

		if (table == null && column == null && key != null && key.compareTo(MetaModelKeywords.COMMENTS) == 0)
			return "Default extension comment for database";

		if (table != null && column == null && key != null && key.compareTo(MetaModelKeywords.COMMENTS) == 0)
			return "Default extension comment for a table";

		if (table != null && column != null && key != null && key.compareTo(MetaModelKeywords.COMMENTS) == 0)
			return "Default extension comment for a column";

		return null;
	}

	public Map<String, String> get(String table, String column) {

		
		if (table == null && column == null)
		{
			
		}
		if (table != null && column == null)
		{
			
		}

		if (table != null && column != null)
		{
			Map<String, String> map = new HashMap<String, String>();
			map.put("reviewed", "--");
			map.put("reviewedBy", "Not Specified");
			map.put("dataClassification", "Confidential");
			map.put("masking", "--None");
			map.put("consumer", "--None");
			map.put("source", "external");
			map.put("information", "http://www.google.com.au");
			return map;
		}


		return null;
	}

	public List<String> getTables() {

		List<String> list = new ArrayList<String>();
		for (TableMetadata table : tables)
			list.add(table.getName());
		return list;
	}

	public List<String> getColumns(String tableName) {

		for (TableMetadata table : tables) {
			if (tableName.compareToIgnoreCase(table.getName()) == 0) {
				List<String> list = new ArrayList<String>();
				for (ColumnMetadata col : table.getColumns())
					list.add(col.getName());
				return list;
			}
		}

		return null;
	}

}
