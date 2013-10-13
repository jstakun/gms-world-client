/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

/**
 *
 * @author jstakun
 */
public class GeonamesReader extends AbstractSerialReader {

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		String url = ConfigurationManager.getInstance().getServicesUrl() + "geonamesProvider";
		return parser.parse(url, params, landmarks, task, true, null);
	}

    /*private static final String[] prefixes = new String[]{"http://"};

    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);

        String url = ConfigurationManager.getInstance().getServicesUrl() + "geonamesProvider?" +
                     "latitude=" + coords[0] + "&longitude=" + coords[1] + "&radius=" + radius + 
                     "&version=3" + "&limit=" + limit + "&display=" + display;

        return parser.parse(url, landmarks, Commons.WIKIPEDIA_LAYER, prefixes, -1, -1, task, true, limit);
    }

    @Override
    public String[] getUrlPrefix() {
        return prefixes;
    }*/
}
