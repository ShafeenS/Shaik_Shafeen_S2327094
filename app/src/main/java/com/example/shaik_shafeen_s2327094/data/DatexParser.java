package com.example.shaik_shafeen_s2327094.data;

import com.example.shaik_shafeen_s2327094.model.Impact;
import com.example.shaik_shafeen_s2327094.model.LocationPoint;
import com.example.shaik_shafeen_s2327094.model.Situation;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class DatexParser {


    private DatexParser() { }

    // Parses the XML feed and returns the situations found.
    public static List<Situation> parse(String xml, Situation.Kind kind)
            throws XmlPullParserException, IOException {
        List<Situation> result = new ArrayList<>();
        if (xml == null || xml.trim().isEmpty()) return result;

        // Removes any unwanted text before the XML declaration.
        int start = xml.indexOf("<?");
        if (start > 0) xml = xml.substring(start);

        // Sets up the XML parser.
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(xml));

        List<String> path = new ArrayList<>();   // names of the currently open tags
        Situation current = null;
        LocationPoint point = null;

        int event = xpp.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                path.add(name);
                if (name.equals("situationRecord")) {
                    current = new Situation(kind);
                    current.setId(attribute(xpp, "id"));
                    current.setRecordType(attribute(xpp, "type"));
                } else if (current != null && isPointRole(name)
                        && path.contains("groupOfLocations")) {
                    point = new LocationPoint(name);
                }
            } else if (event == XmlPullParser.TEXT) {
                if (current != null) {
                    String text = xpp.getText().trim();
                    if (!text.isEmpty()) handleText(current, point, path, text);
                }
            } else if (event == XmlPullParser.END_TAG) {
                String name = xpp.getName();
                if (name.equals("situationRecord") && current != null) {
                    current.finish();
                    result.add(current);
                    current = null;
                    point = null;
                } else if (current != null && point != null && name.equals(point.getRole())) {
                    current.addPoint(point);
                    point = null;
                }
                if (!path.isEmpty()) path.remove(path.size() - 1);
            }
            event = xpp.next();
        }
        return result;
    }

    // Checks if the tag represents a location point.
    private static boolean isPointRole(String name) {
        return name.equals("from") || name.equals("to")
                || name.equals("point") || name.equals("locationForDisplay");
    }

    // Reads an attribute from an XML element.
    private static String attribute(XmlPullParser xpp, String wanted) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (wanted.equals(xpp.getAttributeName(i))) return xpp.getAttributeValue(i);
        }
        return "";
    }

    // Extracts the different details from the XML.
    private static void handleText(Situation s, LocationPoint point,
                                   List<String> path, String text) {
        String name = path.get(path.size() - 1);
        String parent = path.size() > 1 ? path.get(path.size() - 2) : "";

        if (point != null) {                       // inside from / to / point
            switch (name) {
                case "latitude":  point.setLatitude(toDouble(text));  break;
                case "longitude": point.setLongitude(toDouble(text)); break;
                case "value":     point.addName(text);                break;
                default: break;
            }
            return;
        }

        Impact impact = s.getImpact();
        switch (name) {
            case "value":
                if (path.contains("generalPublicComment") && s.getComment().isEmpty()) {
                    s.setComment(text);
                }
                break;
            case "overallStartTime":  s.setStartTime(text);  break;
            case "overallEndTime":    s.setEndTime(text);    break;
            case "validityStatus":    s.setValidityStatus(text); break;
            case "situationRecordCreationTime": s.setCreationTime(text); break;
            case "situationRecordVersionTime":  s.setVersionTime(text);  break;
            case "probabilityOfOccurrence":     s.setProbability(text);  break;
            case "complianceOption":  s.setCompliance(text); break;
            case "tpegDirection":     s.setDirection(text);  break;
            case "urgentRoadworks":   s.setUrgent(Boolean.parseBoolean(text)); break;
            case "delaysType":        impact.setDelaysType(text); break;
            case "delayTimeValue":    impact.setDelaySeconds(toInt(text)); break;
            case "numberOfLanesRestricted":  impact.setLanesRestricted(toInt(text));  break;
            case "numberOfOperationalLanes": impact.setOperationalLanes(toInt(text)); break;
            default:
                // Handles other types of situations.
                if (parent.equals("situationRecord") && name.endsWith("Type")) {
                    s.setSubType(text);
                }
                break;
        }
    }

    // Converts the text into a double value.
    private static double toDouble(String t) {
        try { return Double.parseDouble(t); } catch (NumberFormatException e) { return Double.NaN; }
    }

    // Converts the text into an integer value.
    private static int toInt(String t) {
        try { return Integer.parseInt(t); } catch (NumberFormatException e) { return -1; }
    }
}
