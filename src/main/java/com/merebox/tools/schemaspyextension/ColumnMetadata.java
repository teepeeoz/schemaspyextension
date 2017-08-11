package com.merebox.tools.schemaspyextension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.schemaspy.model.xml.MetaModelKeywords;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ColumnMetadata {

    private final String name;
    private final String type;
    private final boolean isPrimary;
    private final String id;
    private final int size;
    private final int digits;
    private final boolean isNullable;
    private final String comments;
    private final String defaultValue;
    private final boolean isAutoUpdated;
    private final boolean isExcluded;
    private final boolean isAllExcluded;
    private final boolean isImpliedParentsDisabled;
    private final boolean isImpliedChildrenDisabled;
    

    public ColumnMetadata(Node colNode) {
        NamedNodeMap attribs = colNode.getAttributes();
        String tmp;

        name = attribs.getNamedItem("name").getNodeValue();

        Node node = attribs.getNamedItem(MetaModelKeywords.COMMENTS);
        if (node == null)
            node = attribs.getNamedItem("remarks");
        if (node != null) {
            tmp = node.getNodeValue().trim();
            comments = tmp.length() == 0 ? null : tmp;
        } else {
            comments = null;
        }

        node = attribs.getNamedItem("type");
        type = node == null ? "Unknown" : node.getNodeValue();

        node = attribs.getNamedItem("id");
        id = node == null ? null : node.getNodeValue();

        node = attribs.getNamedItem("size");
        size = node == null ? 0 : Integer.parseInt(node.getNodeValue());

        node = attribs.getNamedItem("digits");
        digits = node == null ? 0 : Integer.parseInt(node.getNodeValue());
        
        node = attribs.getNamedItem("nullable");
        isNullable = node != null && evalBoolean(node.getNodeValue());

        node = attribs.getNamedItem("autoUpdated");
        isAutoUpdated = node != null && evalBoolean(node.getNodeValue());
        
        node = attribs.getNamedItem("primaryKey");
        isPrimary = node != null && evalBoolean(node.getNodeValue());
        
        node = attribs.getNamedItem("defaultValue");
        defaultValue = node == null ? null : node.getNodeValue();
        
        node = attribs.getNamedItem("disableImpliedKeys");
        if (node != null) {
            tmp = node.getNodeValue().trim().toLowerCase();
            switch (tmp) {
                case "to":
                    isImpliedChildrenDisabled = true;
                    isImpliedParentsDisabled = false;
                    break;
                case "from":
                    isImpliedParentsDisabled = true;
                    isImpliedChildrenDisabled = false;
                    break;
                case "all":
                    isImpliedChildrenDisabled = isImpliedParentsDisabled = true;
                    break;
                default:
                    isImpliedChildrenDisabled = isImpliedParentsDisabled = false;
                    break;
            }
        } else {
            isImpliedChildrenDisabled = isImpliedParentsDisabled = false;
        }

        node = attribs.getNamedItem("disableDiagramAssociations");
        if (node != null) {
            tmp = node.getNodeValue().trim().toLowerCase();
            switch (tmp) {
                case "all":
                    isAllExcluded = true;
                    isExcluded = true;
                    break;
                case "exceptdirect":
                    isAllExcluded = false;
                    isExcluded = true;
                    break;
                default:
                    isAllExcluded = false;
                    isExcluded = false;
                    break;
            }
        } else {
            isAllExcluded = false;
            isExcluded = false;
        }



    }

    private boolean evalBoolean(String exp) {
        if (exp == null)
            return false;

        String returnExp = exp.trim().toLowerCase();
        return "true".equals(returnExp) || "yes".equals(returnExp) || "1".equals(returnExp);
    }
    

    public String getName() {
        return name;
    }

    public String getComments() {
        return comments;
    }
    
}
