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
import java.util.logging.LogManager;
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
import org.schemaspy.model.xml.MetamodelFailure;
import org.schemaspy.model.xml.ModelExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author tomp
 *
 */
public class GenericMeta implements ModelExtension {

	private static String VERSION_STRING = "Generic metamodel plugin v.0.0.1";
	private static String SCHEMA_XSD = "schemaspy.generic.meta.xsd";
	private static String LOGGING_PROPERTY_FILE = "logging.properties";

	private final static String DEFAULT_SCHEMA = "PUBLIC";
	private final static String DEFAULT_CATALOG = "CATALOG";

	private final static String RESERVED_CATALOG = "catalog";
	private final static String RESERVED_SCHEMA = "schema";
	private final static String RESERVED_TABLE = "table";
	private final static String RESERVED_COLUMN = "column";

	private final static String LEFT_LABEL = "leftLabel";
	private final static String RIGHT_LABEL = "rightLabel";
	private final static String HEADING = "heading";

	private Document extensionDocument;
	private List<TableMetadata> tables = new ArrayList<TableMetadata>();
	private List<String> mTables = new ArrayList<String>();
	private List<String> mColumns = new ArrayList<String>();
	private String comments;
	private File metaFile;
	private InputStream xslStream;

	// Due to Java looging class loading issues handle logging manually
	private Logger logger;

	public GenericMeta() {

		if (logger == null) {
			File logProperty = new File(LOGGING_PROPERTY_FILE);
			if (logProperty.exists()) {
				try {

					InputStream inputStream = new FileInputStream(logProperty);
					LogManager.getLogManager().readConfiguration(inputStream);
					logger = Logger.getLogger(getClass().getName());
					logger.fine("Loaded logging property file");

				} catch (final IOException ioe) {
					Logger.getAnonymousLogger().severe("Could not load default logging.properties file");
					Logger.getAnonymousLogger().severe(ioe.getMessage());
				}
			}
		}

		if (logger == null) {
			logger = Logger.getLogger(getClass().getName());
			logger.fine("Used default logging");
		}
		logger.fine("Initialising GenericMeta");
		// No actions required
	}

	public void loadModelExtension(String xmlMeta, String dbName, String schema) throws InvalidConfigurationException {

		logger.info("Load model extension");
		if (schema == null || schema.isEmpty())
			schema = DEFAULT_SCHEMA;

		if (dbName == null || dbName.isEmpty())
			dbName = schema;

		String catalog = DEFAULT_CATALOG;

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
								"Meta directory \"" + xmlMeta + "\" should contain a file named \"" + filename + "\"");
						comments = null;
						metaFile = null;
						return;
					}

					throw new InvalidConfigurationException(
							"Meta directory \"" + xmlMeta + "\" must contain a file named \"" + filename + "\"");
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
					logger.info("Using XSD file: \"" + xsd.getCanonicalPath() + "\"");
				} catch (FileNotFoundException fnfe) {
					throw new InvalidConfigurationException(
							"Schema XSD file (" + xsd.getAbsolutePath() + ") failed to load");
				} catch (IOException ie) {
					logger.warning("Error in using XSD file, though stream load successful");
				}
			} else {
				// Note that this in this JARs resource if no file found
				logger.info("Using XSD resource file");
				xslStream = getClass().getResourceAsStream("/" + SCHEMA_XSD);
			}

			extensionDocument = parse(metaFile);

			NodeList commentsNodes = extensionDocument.getElementsByTagName("comments");
			if (commentsNodes != null && commentsNodes.getLength() > 0)
				comments = commentsNodes.item(0).getTextContent();
			else
				comments = null;

			NodeList tablesNodes = extensionDocument.getElementsByTagName("tables");
			if (tablesNodes != null) {
				NodeList tableNodes = ((Element) tablesNodes.item(0)).getElementsByTagName("table");

				for (int i = 0; i < tableNodes.getLength(); ++i) {
					Node tableNode = tableNodes.item(i);
					NamedNodeMap attribs = tableNode.getAttributes();
					String tName = attribs.getNamedItem("name").getNodeValue();
					mTables.add(catalog + ":" + schema + ":" + tName);
					loadColumns(catalog, schema, tableNode);
				}
			}
		} else {
			logger.info("Using no Meta file");

			// Load the shell of the tables
			ArrayList<String> ts = new ArrayList<String>();
		}

	}

	public String getValue(String shema, String table, String column, String key) throws MetamodelFailure {

		// Ensure that key vale is not a special term
		if (key == null)
			return null;

		if ((key.compareToIgnoreCase(RESERVED_CATALOG) == 0) || (key.compareToIgnoreCase(RESERVED_SCHEMA) == 0)
				|| (key.compareToIgnoreCase(RESERVED_TABLE) == 0) || (key.compareToIgnoreCase(RESERVED_COLUMN) == 0))
			throw new MetamodelFailure("Attempted use of reserved keyword for key value. Key value was: " + key);

		if (table == null && column == null && key != null && key.compareTo(MetaModelKeywords.COMMENTS) == 0) {
			return null;
		}

		if (table != null && column == null && key != null) {
			logger.fine("Retrieving meta data for columns in table "+ shema + "." + table);
			if (extensionDocument != null) {
				NodeList tablesNodes = extensionDocument.getElementsByTagName("tables");
				if (tablesNodes != null) {
					NodeList tableNodes = ((Element) tablesNodes.item(0)).getElementsByTagName("table");

					for (int i = 0; i < tableNodes.getLength(); ++i) {
						Node tableNode = tableNodes.item(i);
						TableMetadata tableMeta = new TableMetadata(tableNode);
						if (tableMeta.getName().compareToIgnoreCase(table) == 0) {
							// Special hardcoded handling of "Comments"
							if (key.compareTo(MetaModelKeywords.COMMENTS) == 0)
								return tableMeta.getComments();

							NodeList nodes = ((Element) tableNode).getElementsByTagName(key);
							if (nodes != null && nodes.getLength() > 0) {
								return nodes.item(0).getNodeValue();
							}

							break;
						}
					}
				}
			}

			return null;
		}

		if (table != null && column != null && key != null) {
			if (extensionDocument != null) {
				NodeList tablesNodes = extensionDocument.getElementsByTagName("tables");
				if (tablesNodes != null) {
					NodeList tableNodes = ((Element) tablesNodes.item(0)).getElementsByTagName("table");

					for (int i = 0; i < tableNodes.getLength(); ++i) {
						Node tableNode = tableNodes.item(i);
						TableMetadata tableMeta = new TableMetadata(tableNode);
						if (tableMeta.getName().compareToIgnoreCase(table) == 0) {

							NodeList columnNodes = ((Element) tableNode.getChildNodes()).getElementsByTagName("column");
							for (int j = 0; j < columnNodes.getLength(); ++j) {
								Node colNode = columnNodes.item(j);
								ColumnMetadata columnMeta = new ColumnMetadata(colNode);
								if (columnMeta.getName().compareToIgnoreCase(column) == 0) {
									// Special hardcoded handling of "Comments"
									if (key.compareTo(MetaModelKeywords.COMMENTS) == 0)
										return columnMeta.getComments();

									NodeList nodes = ((Element) colNode).getElementsByTagName(key);
									if (nodes != null && nodes.getLength() > 0) {
										return nodes.item(0).getNodeValue();
									}

									break;
								}
							}
						}
					}
				}
			}
			return null;
		}

		return null;
	}

	public Map<String, String> get(String schema, String table, String column) {

		if (table == null && column == null) {
			Map<String, String> map = new HashMap<String, String>();

			return map;
		}

		if (table != null && column == null) {
			Map<String, String> map = new HashMap<String, String>();
			if (extensionDocument != null) {
				NodeList tablesNodes = extensionDocument.getElementsByTagName("tables");
				if (tablesNodes != null) {
					NodeList tableNodes = ((Element) tablesNodes.item(0)).getElementsByTagName("table");

					for (int i = 0; i < tableNodes.getLength(); ++i) {
						Node tableNode = tableNodes.item(i);
						TableMetadata tableMeta = new TableMetadata(tableNode);
						if (tableMeta.getName().compareToIgnoreCase(table) == 0) {
							// Build map of attributes and elements associated
							// with column
							NodeList nodes = tableNode.getChildNodes();
							for (int q = 0; q < nodes.getLength(); ++q) {
								Node item = nodes.item(q);
								if (item.getNodeName().compareTo(RESERVED_COLUMN) != 0)
									if (item.getNodeType() == Node.ELEMENT_NODE)
										map.put(item.getNodeName(), item.getTextContent());
							}
							NamedNodeMap attr = tableNode.getAttributes();
							for (int q = 0; q < attr.getLength(); ++q) {
								Node item = attr.item(q);
								if (item.getNodeName().compareTo(RESERVED_COLUMN) != 0)
									map.put(item.getNodeName(), item.getNodeValue());
							}
							break;
						}
					}
				}
			}

			if (map.isEmpty()) {
				// No values
				// TODO : seek default values for meta data
			}
			return map;
		}

		if (table != null && column != null) {
			Map<String, String> map = new HashMap<String, String>();
			if (extensionDocument != null) {
				NodeList tablesNodes = extensionDocument.getElementsByTagName("tables");
				if (tablesNodes != null) {
					NodeList tableNodes = ((Element) tablesNodes.item(0)).getElementsByTagName("table");

					for (int i = 0; i < tableNodes.getLength(); ++i) {
						Node tableNode = tableNodes.item(i);
						TableMetadata tableMeta = new TableMetadata(tableNode);
						if (tableMeta.getName().compareToIgnoreCase(table) == 0) {

							NodeList columnNodes = ((Element) tableNode.getChildNodes()).getElementsByTagName("column");

							for (int j = 0; j < columnNodes.getLength(); ++j) {
								Node colNode = columnNodes.item(j);
								ColumnMetadata columnMeta = new ColumnMetadata(colNode);
								if (columnMeta.getName().compareToIgnoreCase(column) == 0) {
									// Build map of attributes and elements
									// associated with column
									NodeList nodes = colNode.getChildNodes();
									for (int q = 0; q < nodes.getLength(); ++q) {
										Node item = nodes.item(q);
										if (item.getNodeType() == Node.ELEMENT_NODE)
											map.put(item.getNodeName(), item.getTextContent());
									}
									NamedNodeMap attr = colNode.getAttributes();
									for (int q = 0; q < attr.getLength(); ++q) {
										map.put(attr.item(q).getNodeName(), attr.item(q).getNodeValue());
									}
									break;
								}
							}
						}
					}
				}
			}

			if (map.isEmpty()) {
				// No values
				// TODO : seek default values for meta data
			}
			return map;
		}

		return null;
	}

	public List<String> getSchemas() {

		List<String> list = new ArrayList<String>();
		return list;
	}

	public List<String> getTables(String schema) {

		String prefix = DEFAULT_CATALOG + ":" + schema;
		List<String> list = new ArrayList<String>();
		for (String item : mTables) {
			if (item.startsWith(prefix))
				list.add(item.replace(prefix + ":", ""));
		}

		return list;
	}

	public List<String> getColumns(String schema, String tableName) {

		String prefix = DEFAULT_CATALOG + ":" + schema + ":" + tableName;
		logger.fine("Get columns in table "+ schema + "." + tableName);
		List<String> list = new ArrayList<String>();
		for (String item : mColumns) {
			if (item.startsWith(prefix))
				list.add(item.replace(prefix + ":", ""));
		}
		return list;
	}

	public String version() {
		return VERSION_STRING;
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
			doc = docBuilder.parse(file);
			logger.info("Parsed Metamodel file '" + file + "'");
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

	private void loadColumns(String catalog, String schema, Node tableNode) {

		NamedNodeMap attribs = tableNode.getAttributes();
		String tName = attribs.getNamedItem("name").getNodeValue();

		logger.fine("Loading columns for table "+ tName);
		// Add the column names to the list so we don't need to parse the doc
		// when listing
		NodeList columnNodes = ((Element) tableNode.getChildNodes()).getElementsByTagName("column");
		for (int i = 0; i < columnNodes.getLength(); ++i) {
			Node colNode = columnNodes.item(i);
			attribs = colNode.getAttributes();
			String cName = attribs.getNamedItem("name").getNodeValue();
			mColumns.add(catalog + ":" + schema + ":" + tName + ":" + cName);
		}

	}

}
