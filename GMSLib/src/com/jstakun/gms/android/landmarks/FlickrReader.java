/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

import java.util.List;

import org.apache.http.client.utils.URLEncodedUtils;

/**
 *
 * @author jstakun
 */
public class FlickrReader extends AbstractSerialReader {

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		String url = ConfigurationManager.getInstance().getServicesUrl() + "flickrProvider?" + URLEncodedUtils.format(params, "UTF-8");
		return parser.parse(url, landmarks, task, true, null);
	}

    /*public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);

        String url = ConfigurationManager.getInstance().getServicesUrl() + "flickrProvider?" +
                     "latitudeMin=" + coords[0] + "&longitudeMin=" + coords[1] +
                     "&format=json&version=5" + "&limit=" + limit + "&display=" + display + "&radius=" + radius;

        //System.out.println(url);

        return parser.parse(url, landmarks, Commons.FLICKR_LAYER, null, -1, -1, task, true, limit);
    }*/
}
