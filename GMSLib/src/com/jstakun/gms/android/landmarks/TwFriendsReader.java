package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.Token;

public class TwFriendsReader extends AbstractSerialReader {

	private boolean hasToken = false;
	
	@Override
	protected void init(double latitude, double longitude, int zoom) {
		super.init(latitude, longitude, zoom);
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TWEET_AUTH_STATUS)) {
			ISocialUtils twUtils = OAuthServiceFactory.getSocialUtils(Commons.TWITTER);
			Token token = twUtils.getAccessToken();
			if (token != null) {
				params.put("token", token.getToken());
				params.put("secret", token.getSecret());
				hasToken = true;
			}	
		}
	}
	
	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, String layer, GMSAsyncTask<?, ? ,?> task) {
		String errorMessage = null;
        
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TWEET_AUTH_STATUS)) {
			init(latitude, longitude, zoom);
			if (hasToken) {
				errorMessage = parser.parse(getUrls(), 0, params, landmarks, task, true, Commons.TWITTER, true);
			}
		}		
        return errorMessage;
	}
	
	@Override
	protected String[] getUrls() {
		return new  String[] { 
				ConfigurationManager.getInstance().getSecuredServerUrl() + getUri(),
				ConfigurationManager.getInstance().getSecuredRHCloudUrl() + getUri()
		};
	}
	
	@Override
	protected String getUri() {
		return "twFriends";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.TWITTER_LAYER;
	}

	@Override
	public boolean isPrimary() {
    	return false;
    }

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Twitter_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.twitter;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.twitter_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.twitter_128;
	}
}
