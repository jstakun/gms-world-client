/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.StringUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jstakun
 */
public class LayerJSONParser {

    private HttpUtils utils = new HttpUtils();

    public String parse(Map<String, Layer> layers, List<String> excluded, double latitude, double longitude, int zoom, int width, int height) {
        String errorMessage = null;

        try {
            int radius = DistanceUtils.radiusInKilometer();

            String url = ConfigurationManager.getInstance().getServicesUrl() + "listLayers?format=json&" +
                         "latitudeMin=" + StringUtil.formatCoordE6(latitude) + "&longitudeMin=" + StringUtil.formatCoordE6(longitude) + "&version=2&radius=" + radius;

            byte[] resp = utils.loadHttpFile(url, true, "json");
            if (resp != null && resp.length > 0) {
                //long start = System.currentTimeMillis();
                String jsonResp = new String(resp, "UTF-8");
                //System.out.println("Json: " + jsonResp);
                JSONObject jsonRoot = new JSONObject(jsonResp);

                if (jsonRoot.has("ResultSet")) {
                    parseJSonArray(jsonRoot, layers, excluded);
                }
                //System.out.println("Processed layer " + layer + " containing " + landmarks.size() + " in " + (System.currentTimeMillis() - start) + " millis.");
            }
        } catch (Exception ex) {
            LoggerUtils.error("JSonParser error: ", ex);
            errorMessage = utils.getResponseCodeErrorMessage();
        } finally {
            close();
        }

        return errorMessage;
    }

    private void parseJSonArray(JSONObject jsonRoot, Map<String, Layer> layers, List<String> excluded) throws JSONException {
        JSONArray jsonLayers = jsonRoot.getJSONArray("ResultSet");

        for (int i = 0; i < jsonLayers.length(); i++) {
            JSONObject layer = (JSONObject) jsonLayers.getJSONObject(i);

            String lname = layer.getString("name");
            String desc = null;
            if (layer.has("desc") && !layer.isNull("desc")) {
                desc = layer.getString("desc");
            }
            String formatted = layer.getString("formatted");
            String icon_uri = layer.getString("iconURI");
            boolean manageable = layer.getBoolean("manageable");
            boolean enabled = layer.getBoolean("enabled");
            boolean checkinable = layer.getBoolean("checkinable");
            if (layer.getBoolean("isEmpty") == true) {
                excluded.add(lname);
            }
            boolean searchable = true;

            List<LayerReader> layerReader = new ArrayList<LayerReader>();
            layerReader.add(new GMSWorldReader());
            Layer l = LayerFactory.getLayer(lname, false, manageable, enabled, checkinable, searchable, layerReader, icon_uri, -1, null, -1, LayerManager.LAYER_EXTERNAL, desc, formatted);

            if (!(layer == null || l.getName().equals(Commons.LM_SERVER_LAYER))) {
                LoggerUtils.debug("Adding layer: " + l.getName());
                layers.put(l.getName(), l);
            }
        }
    }

    public void close() {
        try {
            if (utils != null) {
                utils.close();
            }
        } catch (IOException ex) {
            LoggerUtils.debug("JSonParser error: ", ex);
        }
    }
}
