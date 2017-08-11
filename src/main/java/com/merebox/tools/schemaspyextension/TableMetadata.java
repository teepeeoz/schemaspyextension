package com.merebox.tools.schemaspyextension;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.schemaspy.model.xml.MetaModelKeywords;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TableMetadata {

    private final String name;
    private final String comments;
    private final List<ColumnMetadata> columns = new ArrayList<ColumnMetadata>();
    private final String remoteCatalog;
    private final String remoteSchema;
    	

	TableMetadata(Node tableNode) {
        NamedNodeMap attribs = tableNode.getAttributes();

        name = attribs.getNamedItem("name").getNodeValue();

        Node node = attribs.getNamedItem(MetaModelKeywords.COMMENTS);
        if (node == null)
            node = attribs.getNamedItem("remarks");
        if (node != null) {
            String tmp = node.getNodeValue().trim();
            comments = tmp.length() == 0 ? null : tmp;
        } else {
            comments = null;
        }

        node = attribs.getNamedItem(MetaModelKeywords.REMOTE_SCHEMA);
        remoteSchema = node == null ? null : node.getNodeValue().trim();

        node = attribs.getNamedItem(MetaModelKeywords.REMOTE_CATALOG);
        remoteCatalog = node == null ? null : node.getNodeValue().trim();

        NodeList columnNodes = ((Element)tableNode.getChildNodes()).getElementsByTagName("column");

        for (int i = 0; i < columnNodes.getLength(); ++i) {
            Node colNode = columnNodes.item(i);
            columns.add(new ColumnMetadata(colNode));
        }
    }
	

    public String getName() {
        return name;
    }
    
    public List<ColumnMetadata> getColumns() {
        return columns;
    }


	public String getComments() {
		return comments;
	}


}
