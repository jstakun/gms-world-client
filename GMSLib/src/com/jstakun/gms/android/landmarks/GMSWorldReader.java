/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

/**
 *
 * @author jstakun
 */
public class GMSWorldReader extends AbstractSerialReader {
    
	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		params.add(new BasicNameValuePair("latitudeMin", Double.toString(latitude)));
		params.add(new BasicNameValuePair("longitudeMin", Double.toString(longitude)));
	}
	
	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
		init(latitude, longitude, zoom, width, height);
		params.add(new BasicNameValuePair("layer", layer));
	    return parser.parse(getUrl(), params, landmarks, task, true, layer);	
    }


	@Override
	protected String getUrl() {
		return ConfigurationManager.getInstance().getServicesUrl() + "downloadLandmark";
	}
}
