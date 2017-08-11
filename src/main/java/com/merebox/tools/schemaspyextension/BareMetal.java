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
public class BareMetal implements ModelExtension {


	public BareMetal()
	{
		// No actions required
	}
	
	private void validate(Document document) throws SAXException, IOException {

	}

	private Document parse(File file) throws InvalidConfigurationException {
		DocumentBuilder docBuilder;
		Document doc = null;
		return doc;
	}

	public void loadModelExtension(String xmlMeta, String dbName, String schema) throws InvalidConfigurationException {

	}

	public String getValue(String shema, String table, String column, String key) {

		return null;
	}

	public Map<String, String> get(String schema, String table, String column) {

		return null;
	}

	public List<String> getSchemas() {

		List<String> list = new ArrayList<String>();
		return list;
	}

	public List<String> getTables(String schema) {

		List<String> list = new ArrayList<String>();
		return list;
	}

	public List<String> getColumns(String schema, String tableName) {

		return null;
	}

	public String version() {
		return "Bare metamodel plugin v.0.0.1";
	}

}
