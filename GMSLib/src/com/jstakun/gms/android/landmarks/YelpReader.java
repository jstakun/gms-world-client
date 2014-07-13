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
public class YelpReader extends AbstractSerialReader{

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		int dist = radius * 1000;
		params.remove(0); //remove default radius parameter
		params.add(new BasicNameValuePair("radius", Integer.toString(dist)));
	}

	@Override
	protected String getUri() {
		return "yelpProvider";
	}
}
