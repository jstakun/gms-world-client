package com.jstakun.gms.android.landmarks;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.StringUtil;

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

            String url = ConfigurationManager.getInstance().getServerUrl() + "listLayers";

            Map<String, String> params = new HashMap<String, String>();
	    	params.put("format","json");
			params.put("latitudeMin",StringUtil.formatCoordE6(latitude));
			params.put("longitudeMin", StringUtil.formatCoordE6(longitude));
			params.put("version","2");
			params.put("radius",Integer.toString(radius));
            
			String jsonResp = utils.sendPostRequest(url, params, true);
            
            int responseCode = utils.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK && StringUtils.startsWith(jsonResp, "{")) {
                JSONObject jsonRoot = new JSONObject(jsonResp);
                if (jsonRoot.has("ResultSet")) {
                    parseJSonArray(jsonRoot, layers, excluded);
                }
            } else {
            	LoggerUtils.error("Received server response " + responseCode + ": " + jsonResp);
            }
        } catch (Exception ex) {
            LoggerUtils.error("LayerJSONParser.parse() exception: ", ex);
            errorMessage = utils.getResponseCodeErrorMessage();
        } finally {
            close();
        }

        return errorMessage;
    }

    private void parseJSonArray(JSONObject jsonRoot, Map<String, Layer> layers, List<String> excluded) throws JSONException {
        JSONArray jsonLayers = jsonRoot.getJSONArray("ResultSet");

        for (int i = 0; i < jsonLayers.length(); i++) {
            JSONObject layer = jsonLayers.getJSONObject(i);

            String desc = null;
            if (layer.has("desc") && !layer.isNull("desc")) {
                desc = layer.getString("desc");
            }
            String formatted = layer.getString("formatted");
            String icon_uri = layer.getString("iconURI");
            boolean manageable = layer.getBoolean("manageable");
            boolean enabled = layer.getBoolean("enabled");
            boolean checkinable = layer.getBoolean("checkinable");
            String lname = layer.getString("name");
            if (layer.getBoolean("isEmpty")) {
                excluded.add(lname);
            }
            boolean searchable = true;

            List<LayerReader> layerReader = new ArrayList<LayerReader>();
            layerReader.add(new GMSWorldReader());
            Layer l = LayerFactory.getLayer(lname, false, manageable, enabled, checkinable, searchable, layerReader, icon_uri, -1, null, -1, LayerManager.LAYER_EXTERNAL, desc, formatted, FileManager.ClearPolicy.ONE_DAY, 0);

            if (! StringUtils.equals(l.getName(),Commons.LM_SERVER_LAYER)) {
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
