/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author jstakun
 */
public class FbCheckinsReader extends AbstractSerialReader {

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
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
		String errorMessage = null;
        
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
			init(latitude, longitude, zoom, width, height);
			if (hasToken) {
				errorMessage = parser.parse(getUrls(), 0, params, landmarks, task, true, Commons.FACEBOOK);
			}
		}
		
        return errorMessage;
	}

	@Override
	protected String[] getUrls() {
		return new String[]{ 
				ConfigurationManager.getInstance().getSecuredServerUrl() + getUri(),
				ConfigurationManager.getInstance().getSecuredRHCloudUrl() + getUri()
		};
	}
	
	@Override
	protected String getUri() {
		return "fbCheckins";
	}
}
