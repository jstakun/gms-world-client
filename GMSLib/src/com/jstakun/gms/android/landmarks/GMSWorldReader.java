/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import org.apache.http.message.BasicNameValuePair;

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
	protected String getUri() {
		return "downloadLandmark";
		//return "http://landmarks-gmsworld.rhcloud.com/actions/layersProvider";
	}
}
