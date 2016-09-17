package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;

/**
 *
 * @author jstakun
 */
public class FbPlacesReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
			ISocialUtils fbUtils = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK);
            String token = fbUtils.getAccessToken().getToken();
            if (token != null) {
            	params.put("token", token);
            }
		}
	}
	
    @Override
	protected String getUri() {
    	return "facebookProvider";
	}
}
