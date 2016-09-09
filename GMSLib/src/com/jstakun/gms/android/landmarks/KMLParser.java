package com.jstakun.gms.android.landmarks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.openlapi.QualifiedCoordinates;

/**
 *
 * @author jstakun
 */
public class KMLParser {

    private static final String KML2_2NSO = "http://www.opengis.net/kml/2.2";
    private static final String KML2_2NS = "http://earth.google.com/kml/2.2";
    private static final String KML2_1NS = "http://earth.google.com/kml/2.1";
    private static final String KML2_0NS = "http://earth.google.com/kml/2.0";
    private HttpUtils utils = new HttpUtils();
    private String errorMessage = null;
    private List<ExtendedLandmark> origLandmarks = new ArrayList<ExtendedLandmark>();
    private String layer, description, routeName = "Your route";
    private int counter;
    private long creationDate;

    private InputStream getInputStream(String source, String filename, boolean authn) throws Exception {
        InputStream is = null;

        if (source != null && source.startsWith("http")) {
            byte[] file = utils.loadHttpFile(source, authn, "kml");
            errorMessage = utils.getResponseCodeErrorMessage();
            if (file != null) {
                is = new ByteArrayInputStream(file);
            }
            creationDate = System.currentTimeMillis();
        } else {
            File fc = PersistenceManagerFactory.getFileManager().getExternalDirectory(source, filename);
            if (!fc.exists()) {
                throw new Exception("File " + source + "/" + filename + " doesn't exists");
            } else {
                is = new FileInputStream(fc);
                creationDate = fc.lastModified();
            }
        }

        return is;
    }

    public String parse(String source, String filename, List<ExtendedLandmark> landmarks, boolean authn, String layer, GMSAsyncTask<?,?,?> caller) throws Exception {
        InputStream input = null;
        this.layer = layer;
        counter = 0;
       
        if (StringUtils.endsWith(filename, ".kml")) {
        	this.routeName = StringUtils.split(filename, ".")[0];
        } else {
        	this.routeName = filename;
        }
        
        if (!landmarks.isEmpty()) {
            origLandmarks.addAll(landmarks);
        }

        try {
            input = getInputStream(source, filename, authn);
            if (input != null) {
            	XmlPullParser parser = new KXmlParser();
                parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true);
                parser.setInput(input, null);//"UTF-8");
                parser.next();
                // confirm that this is a Google file
                String namespace = parser.getAttributeValue(0);
                if (!(namespace.equals(KML2_2NS) || namespace.equals(KML2_2NSO) || namespace.equals(KML2_1NS) || namespace.equals(KML2_0NS))) {
                    throw new Exception(source + " not a valid KML 2.x file.");
                }
                findPlacemarks(parser, landmarks, caller);
            }
        } catch (XmlPullParserException e) {
            LoggerUtils.error("KMLParser.parse exception", e);
        } catch (IOException e) {
            LoggerUtils.error("KMLParser.parse exception", e);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
                if (utils != null) {
                    utils.close();
                }

            } catch (IOException e) {
            }
        }

        return errorMessage;
    }

    private void findPlacemarks(XmlPullParser parser, List<ExtendedLandmark> landmarks, GMSAsyncTask<?,?,?> caller) throws XmlPullParserException, IOException {
        // loop that increments elements in the XML stream
        int event = parser.next();
        for (; event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event != XmlPullParser.START_TAG) {
                // we're only interested in finding Placemark tags
                continue;
            }
            String name = parser.getName();
            if ("Placemark".equals(name)) {
                // if it's a Placemark parse it, even if it's in a Folder
                List<ExtendedLandmark> lms = parsePlacemark(parser);
                if (!origLandmarks.isEmpty()) {
                    //counter = 0;
                    for (Iterator<ExtendedLandmark> iter = lms.iterator(); iter.hasNext();) {
                        ExtendedLandmark landmark = iter.next();
                        if (!origLandmarks.contains(landmark)) {
                            counter++;
                            LoggerUtils.debug("KMLParser: Adding landmark " + landmark.getName() + "\nLandmark count: " + counter);
                            landmarks.add(landmark);
                            LandmarkManager.getInstance().addLandmarkToDynamicLayer(landmark);
                        }
                    }
                } else if (lms != null) {
                    counter += lms.size();
                    LoggerUtils.debug("KMLParser: creating new Landmarks from Placemark" + "\nLandmark count: " + counter);
                    for (Iterator<ExtendedLandmark> iter = lms.iterator(); iter.hasNext();) {
                        ExtendedLandmark landmark = iter.next();
                        landmarks.add(landmark);
                        LandmarkManager.getInstance().addLandmarkToDynamicLayer(landmark);
                    }
                }
            } else if ("description".equals(name)) {
                int l = parser.next();
                
                if (l == XmlPullParser.TEXT) {
                    description = getUtf8(parser);
                }
            }
            // else ignore it
            if (caller != null && caller.isCancelled()) {
                return;
            }
        }
    }

    /**
     * To be called immediately after the parser reaches the beginning of a LookAt tag,
     * this will extract and return the heading after proceeding to the end of the LookAt
     * element.
     *
     * @return
     * @throws XmlPullParserException
     *             if there are any parse errors, including if the entry for heading is
     *             not a valid floating point number.
     * @throws IOException
     */
     /*private float parseLookAt(XmlPullParser parser) throws XmlPullParserException, IOException {
        // keep track of the tag we are in
        List<String> tags = new ArrayList<String>();
        tags.add("LookAt");

        float heading = Float.NaN;

        int event = parser.next();
        for (; event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                // mark that we are in a tag
                tags.add(name);
                continue;
            } else if (event == XmlPullParser.END_TAG) {
                String name = parser.getName();
                // remove the tag from the list
                tags.remove(name);
                if ("LookAt".equals(name)) // end of LookAt info, return
                {
                    return heading;
                }
            }
            if (event == XmlPullParser.TEXT) {
                // we recorded where we are
                String name = tags.get(tags.size()-1);
                if ("heading".equals(name)) {
                    String content = parser.getText();
                    try {
                        heading = Float.parseFloat(content);
                    } catch (IllegalArgumentException e) {
                        throw new XmlPullParserException(e.getMessage());
                    }
                }
            }
        }
        // we get here if the tag never closed
        throw new XmlPullParserException("expected </LookAt>");
    }*/

    /**
     * Parse a Placemark element into a Location. Moves the parser to the end of the
     * Placemark element.
     *
     * @param element
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    private List<ExtendedLandmark> parsePlacemark(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        String lmname = routeName;
        String lmdescription = null;
        List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();
        List<?> qc = null;

        // keep track of the tag we are in
        List<String> tags = new ArrayList<String>();
        //System.out.println("Adding: Placemark");
        tags.add("Placemark");

        int event = parser.next();

        for (; event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                // mark that we are in a tag
                //System.out.println("Adding: " + name);
                tags.add(name);
                continue;
            } else if (event == XmlPullParser.END_TAG) {
                String name = parser.getName();
                // remove the tag from the list
                //System.out.println("Removing: " + name);
                tags.remove(name);
                //System.out.println("Removed: " + name);
                if ("Placemark".equals(name)) // end of Placemark info, return
                {
                    if (qc == null) {
                        return null;
                    } else {
                        if (lmname == null) {
                            lmname = routeName;
                        }
                        if (lmdescription == null) {
                        	lmdescription = description;
                        }
                        for (int i = 0; i < qc.size(); i++) {
                            //System.out.println(i);
                            landmarks.add(LandmarkFactory.getLandmark(lmname, lmdescription, (QualifiedCoordinates) qc.get(i), layer, creationDate));
                        }
                        return landmarks;
                    }
                }
            }
            if (event == XmlPullParser.TEXT) {
                // we recorded where we are
                String name = tags.get(tags.size()-1);
                //System.out.println(name);
                if ("name".equals(name)) {
                    lmname = getUtf8(parser);
                    tags.remove("name");
                } else if ("description".equals(name)) {
                    // description
                    lmdescription = getUtf8(parser);
                    tags.remove("decription");
                } else if ("LookAt".equals(name)) {
                    // LookAt tag holds course info
                    //location.setCourse(parseLookAt(parser));
                    tags.remove("LookAt");
                } else if ("Point".equals(name)) {
                    // Point
                    qc = parsePoint(parser);
                    tags.remove("Point");
                } else if ("coordinates".equals(name)) {
                    String content = parser.getText();
                    qc = parseCoordinates(content);
                    tags.remove("coordinates");
                }
            }
        }

        // if there were no coordinate, return null
        if (qc == null) {
            return null;
        }

        return null;
    }

    /**
     * Parse a Point element, returning the QualifiedCoordinates contained within.
     *
     * @param entry
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    private List<QualifiedCoordinates> parsePoint(XmlPullParser parser)
            throws XmlPullParserException, IOException {
    	List<QualifiedCoordinates> qc = new ArrayList<QualifiedCoordinates>();
        // keep track of the tag we are in
        List<String> tags = new ArrayList<String>();
        tags.add("Point");

        int event = parser.next();
        for (; event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                // mark that we are in a tag
                //System.out.println("Adding: " + name);
                tags.add(name);
                continue;
            } else if (event == XmlPullParser.END_TAG) {
                String name = parser.getName();
                // remove the tag from the list
                //System.out.println("Removing: " + name);
                tags.remove(name);
                if ("Point".equals(name)) // end of Point info, return
                {
                    return qc;
                }
            }
            if (event == XmlPullParser.TEXT) {
                // we recorded where we are
                String name = tags.get(tags.size()-1);
                //System.out.println(name);
                String content = parser.getText();
                //System.out.println(content);
                if ("coordinates".equals(name)) {
                    try {
                        // unfortunately, the actual data is contained in a
                        // coordinates element, so out source again
                        qc = parseCoordinates(content);
                    } catch (IllegalArgumentException e) {
                        throw new XmlPullParserException(e.getMessage());
                    }
                }
            }
        }
        // we get here if the tag never closed
        throw new XmlPullParserException("expected </Point>");
    }

    private List<QualifiedCoordinates> parseCoordinates(String content)
            throws IllegalArgumentException {
        //System.out.println(content);
        List<QualifiedCoordinates> coords = new ArrayList<QualifiedCoordinates>();
        int subStart = 0;
        int p = 0;
        int length = content.length();
        // EclipseME won't allow a double [] array
        double[] parts = new double[3];

        for (int i = 0; i < length; i++) {
            char character = content.charAt(i);
            if ((i != 0 && ((character == ',') || (character == ' ') || (character == '\n'))) || (i == length - 1)) {
                // end of segment reached
                int end = i;
                if (i == length - 1) {
                    end++;
                }
                String part = content.substring(subStart, end);
                //System.out.println("-" + part + "- -" + p + "- " + i + " " + length + " " + subStart + " " + end);
                try {
                    parts[p] = Double.parseDouble(part);
                } catch (NumberFormatException e) {
                    if (p == 2) {
                        parts[p] = 0.0d;
                    } else {
                        throw e;
                    }
                }
                p++;
                subStart = i + 1;

                if (p == 2 && i == length - 1) {
                    parts[p] = 0.0d;
                    p++;
                }

                if (p == 3) {
                    //System.out.println("Adding new coords");
                    QualifiedCoordinates qc =
                            new QualifiedCoordinates(parts[1],
                            parts[0], (float) parts[2], Float.NaN,
                            Float.NaN);

                    coords.add(qc);
                    p = 0;
                }
            }
        }
        // note that KML is long/lat/alt, whereas Coordinates is lat/long/alt
        // KML doesn't support horizontal/vertical accuracy

        return coords;
    }

    public String getDescription() {
        return description;
    }
    
    private String getUtf8(XmlPullParser parser) throws UnsupportedEncodingException {
    	String enc = parser.getInputEncoding(); 
    	String text = parser.getText();
    	if (text != null && enc != null && !enc.equals("UTF-8")) {
    		byte[] inp = text.getBytes(enc);
    		return new String(inp, "UTF-8");
    	} else {
    		return text;
    	}
    }
}
