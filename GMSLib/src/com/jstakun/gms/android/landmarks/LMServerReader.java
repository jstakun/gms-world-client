/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

import java.util.List;

/**
 *
 * @author jstakun
 */
public class LMServerReader extends AbstractSerialReader {

    //private static final String[] LM_SERVER_PREFIX = new String[] {ConfigurationManager.SERVER_URL, ConfigurationManager.BITLY_URL};

    /*public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        
        String url = ConfigurationManager.getInstance().getServicesUrl() + "downloadLandmark?" +
                     "latitudeMin=" + coords[0] + "&longitudeMin=" + coords[1] + "&layer=" + layer +
                     "&format=json&version=5" + "&limit=" + limit + "&display=" + display + "&radius=" + radius;

        return parser.parse(url, landmarks, layer, LM_SERVER_PREFIX , -1, -1, task, true, limit);
    }
    
    @Override
    public String[] getUrlPrefix() {
        return LM_SERVER_PREFIX;
    }*/

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		String url = ConfigurationManager.getInstance().getServicesUrl() + "downloadLandmark?" +
                "latitudeMin=" + coords[0] + "&longitudeMin=" + coords[1] + "&layer=" + layer +
                "&format=bin&version=" + SERIAL_VERSION + "&limit=" + limit + "&display=" + display + "&radius=" + radius;

		return parser.parse(url, landmarks, task, true, null);
	}
}
