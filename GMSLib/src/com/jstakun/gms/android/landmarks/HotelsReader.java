/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import java.util.Locale;

import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author jstakun
 */
public class HotelsReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);    
		String l = Locale.getDefault().getLanguage();
		params.add(new BasicNameValuePair("latitudeMin", Double.toString(latitude)));
		params.add(new BasicNameValuePair("longitudeMin", Double.toString(longitude)));
		params.add(new BasicNameValuePair("lang", l));
	}

	@Override
	protected String getUri() {
		return "hotelsProvider";
		//return "http://landmarks-gmsworld.rhcloud.com/actions/layersProvider";
	}
}
