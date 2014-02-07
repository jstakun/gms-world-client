/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import java.util.Locale;

import org.apache.http.message.BasicNameValuePair;

import com.jstakun.gms.android.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class GooglePlacesReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		String lang = Locale.getDefault().getLanguage();
		params.add(new BasicNameValuePair("language", lang));
	}

	@Override
	protected String getUrl() {
		return ConfigurationManager.getInstance().getServerUrl() + "googlePlacesProvider";
	}
}
