/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.MercatorUtils;
import com.jstakun.gms.android.utils.StringUtil;

/**
 *
 * @author jstakun
 */
public class OsmReader extends AbstractSerialReader {

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
        BoundingBox bbox = MercatorUtils.getBoundingBox(width, height, latitude, longitude, zoom);
        params.add(new BasicNameValuePair("longitudeMin", StringUtil.formatCoordE2(bbox.west)));
		params.add(new BasicNameValuePair("latitudeMin", StringUtil.formatCoordE2(bbox.south)));
		params.add(new BasicNameValuePair("longitudeMax", StringUtil.formatCoordE2(bbox.east)));  
		params.add(new BasicNameValuePair("latitudeMax", StringUtil.formatCoordE2(bbox.north)));
		
        String amenity = "atm"; //Commons.OSM_ATM_LAYER
        if (layer.equals(Commons.OSM_PARKING_LAYER)){
            amenity = "parking";
        }
        params.add(new BasicNameValuePair("amenity", amenity));
        
		String url = ConfigurationManager.getInstance().getServicesUrl() + "osmProvider";
		return parser.parse(url, params, landmarks, task, true, null);
	}

    /*@Override
    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        BoundingBox bbox = MercatorUtils.getBoundingBox(width, height, latitude, longitude, zoom);
        
        String amenity = "atm"; //Commons.OSM_ATM_LAYER
        if (layer.equals(Commons.OSM_PARKING_LAYER)){
            amenity = "parking";
        }

        String url = ConfigurationManager.getInstance().getServicesUrl() + "osmProvider?" +
                     "latitudeMin=" + StringUtil.formatCoordE2(bbox.south) + "&latitudeMax=" + StringUtil.formatCoordE2(bbox.north) +
                     "&longitudeMin=" + StringUtil.formatCoordE2(bbox.west) + "&longitudeMax=" + StringUtil.formatCoordE2(bbox.east) +
                     "&amenity=" + amenity + "&limit=" + limit + "&display=" + display;

        return parser.parse(url, landmarks, layer, null , -1, -1, task, true, limit);
    }*/
}
