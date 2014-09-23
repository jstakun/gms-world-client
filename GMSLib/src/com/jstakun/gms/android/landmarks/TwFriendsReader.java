package com.jstakun.gms.android.landmarks;

import java.util.List;

import org.apache.http.message.BasicNameValuePair;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.Token;

public class TwFriendsReader extends AbstractSerialReader {

	private boolean hasToken = false;
	
	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TWEET_AUTH_STATUS)) {
			ISocialUtils twUtils = OAuthServiceFactory.getSocialUtils(Commons.TWITTER);
			Token token = twUtils.getAccessToken();
			if (token != null) {
				params.add(new BasicNameValuePair("token", token.getToken()));
				params.add(new BasicNameValuePair("secret", token.getSecret()));
				hasToken = true;
			}	
		}
	}
	
	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
		String errorMessage = null;
        
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TWEET_AUTH_STATUS)) {
			init(latitude, longitude, zoom, width, height);
			if (hasToken) {
				errorMessage = parser.parse(getUrls(), 0, params, landmarks, task, true, Commons.TWITTER);
			}
		}		
        return errorMessage;
	}
	
	@Override
	protected String[] getUrls() {
		return new  String[] { 
				ConfigurationManager.getInstance().getSecuredRHCloudUrl() + getUri(),
				ConfigurationManager.getInstance().getSecuredServerUrl() + getUri() };
	}
	
	@Override
	protected String getUri() {
		return "twFriends";
	}

}
