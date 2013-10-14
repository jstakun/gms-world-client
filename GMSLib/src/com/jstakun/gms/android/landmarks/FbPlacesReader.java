/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import org.apache.http.message.BasicNameValuePair;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;

/**
 *
 * @author jstakun
 */
public class FbPlacesReader extends AbstractSerialReader {

	private boolean hasToken = false;

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
			ISocialUtils fbUtils = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK);
            String token = fbUtils.getAccessToken().getToken();
            if (token != null) {
            	params.add(new BasicNameValuePair("token", token));
            	hasToken = true; 
            }
		}
	}
	
    @Override
	protected String getUrl() {
		if (hasToken) {
			return ConfigurationManager.getInstance().getSecuredServicesUrl() + "facebookProvider";
		} else {
			return ConfigurationManager.SERVER_URL + "facebookProvider";
		}
	}
}
