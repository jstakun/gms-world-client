/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import java.util.List;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.MercatorUtils;
import com.jstakun.gms.android.utils.StringUtil;

/**
 *
 * @author jstakun
 */

public class PanoramioReader extends AbstractSerialReader {

    /*public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {

        init(latitude, longitude, zoom, width, height);
        BoundingBox bbox = MercatorUtils.getBoundingBox(width, height, latitude, longitude, zoom);
        
        String panoramioURL = ConfigurationManager.getInstance().getServicesUrl() + "panoramio2Provider?" +
        "minx=" + StringUtil.formatCoordE2(bbox.west) + "&miny=" + StringUtil.formatCoordE2(bbox.south) + "&maxx=" +
        StringUtil.formatCoordE2(bbox.east) + "&maxy=" + StringUtil.formatCoordE2(bbox.north) + "&limit=" + limit + "&display=" + display + "&version=3";

        //System.out.println(panoramioURL);

        return parser.parse(panoramioURL, landmarks, Commons.PANORAMIO_LAYER, null, -1, -1, task, true, limit);
    }*/

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		BoundingBox bbox = MercatorUtils.getBoundingBox(width, height, latitude, longitude, zoom);
		params.add(new BasicNameValuePair("minx", StringUtil.formatCoordE2(bbox.west)));
		params.add(new BasicNameValuePair("miny", StringUtil.formatCoordE2(bbox.south)));
		params.add(new BasicNameValuePair("maxx", StringUtil.formatCoordE2(bbox.east)));  
		params.add(new BasicNameValuePair("maxy", StringUtil.formatCoordE2(bbox.north)));
		String url = ConfigurationManager.getInstance().getServicesUrl() + "panoramio2Provider?" + URLEncodedUtils.format(params, "UTF-8");
		return parser.parse(url, landmarks, task, true, null);
	}
}
