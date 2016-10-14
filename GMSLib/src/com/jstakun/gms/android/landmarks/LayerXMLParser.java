package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/**
 *
 * @author jstakun
 */
public class LayerXMLParser {

     private static final String LAYER_ELEMENT =  "layer";
     private static final String ICON_URI_ELEMENT =  "iconURI";
     private static final String MANAGEABLE_ELEMENT =  "manageable";
     private static final String ENABLED_ELEMENT =  "enabled";
     private static final String CHECKINABLE_ELEMENT =  "enabled";
     private static final String NAME_ELEMENT =  "name";
     private HttpUtils utils;

     public LayerXMLParser() {
         utils = new HttpUtils();
     }

     public String parse(Map<String,Layer> layers) {
        InputStream input = null;
        String errorMessage = null;
        String url = ConfigurationManager.getInstance().getServerUrl() + "listLayers";
        
        try {
            byte[] file = utils.loadFile(url, true, null, "xml");
            
            if (file != null)
            {
                input = new ByteArrayInputStream(file);
                XmlPullParser parser = new KXmlParser();
                parser.setInput(input, "UTF-8");
                findLayers(parser, layers);
            }
        } catch (Exception e) {
            LoggerUtils.error("LayerParser.parse exception", e);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
                if (utils != null) {
                    errorMessage = utils.getResponseErrorMessage(url);
                    utils.close();
                }
            } catch (IOException e) {
            }
        }

        return errorMessage;
    }

    private void findLayers(XmlPullParser parser, Map<String,Layer> layers) throws XmlPullParserException, IOException {
        int event = parser.next();
        for (; event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (LAYER_ELEMENT.equals(name)) {
                Layer layer = parsePost(parser);
                if (!(layer == null || layer.getName().equals(Commons.LM_SERVER_LAYER))) {
                    LoggerUtils.debug("Adding layer: " + layer.getName());
                    layers.put(layer.getName(), layer);
                }
            }
        }
    }

    private Layer parsePost(XmlPullParser parser) throws XmlPullParserException, IOException {
        String lname = "Noname";
        String icon_uri = null;
        boolean manageable = false;
        boolean enabled = true;
        boolean checkinable = true;

        Vector<String> tags = new Vector<String>();
        tags.addElement(LAYER_ELEMENT);

        int event = parser.next();
        for (; event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event == XmlPullParser.START_TAG) {
                String name = parser.getName();
                // mark that we are in a tag
                tags.addElement(name);
                continue;
            } else if (event == XmlPullParser.END_TAG) {
                String name = parser.getName();
                tags.removeElement(name);
                if (LAYER_ELEMENT.equals(name))
                {
                   boolean searchable = true;
                   List<LayerReader> layerReader = new ArrayList<LayerReader>();
                   layerReader.add(new GMSWorldReader());
                   return LayerFactory.getLayer(lname, manageable, enabled, checkinable, searchable, layerReader, icon_uri, -1, null, -1, LayerManager.LAYER_EXTERNAL, null, lname, FileManager.ClearPolicy.ONE_DAY, 0);
                }
            }
            if (event == XmlPullParser.TEXT) {
                String name = tags.lastElement();
                if (NAME_ELEMENT.equals(name)) {
                    lname = parser.getText();
                    if (lname == null || lname.length() == 0) {
                        lname = "Noname";
                    }
                } else if (ICON_URI_ELEMENT.equals(name)) {
                    icon_uri = parser.getText();
                } else if (ENABLED_ELEMENT.equals(name)) {
                    enabled = ((parser.getText().equals("true")) ? true : false);
                } else if (MANAGEABLE_ELEMENT.equals(name)) {
                    manageable = ((parser.getText().equals("true")) ? true : false);
                } else if (CHECKINABLE_ELEMENT.equals(name)) {
                    checkinable = ((parser.getText().equals("true")) ? true : false);
                }
            }
        }

        return null;
    }

}
