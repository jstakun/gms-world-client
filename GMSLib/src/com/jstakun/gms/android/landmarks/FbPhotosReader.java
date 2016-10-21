package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.GMSAsyncTask;

/**
 *
 * @author jstakun
 */
public class FbPhotosReader extends AbstractSerialReader {
	
	private boolean hasToken = false;

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
			ISocialUtils fbUtils = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK);
            String token = fbUtils.getAccessToken().getToken();
            if (token != null) {
            	params.put("token", token);
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
				errorMessage = parser.parse(getUrls(), 0, params, landmarks, task, true, Commons.FACEBOOK, true);
			}
		}
		
        return errorMessage;
	}

	@Override
	protected String[] getUrls() {
		return new String[] { 
				ConfigurationManager.getInstance().getSecuredServerUrl() + getUri(),
				ConfigurationManager.getInstance().getSecuredRHCloudUrl() + getUri()
		};
	}
	
	@Override
	protected String getUri() {
		return "fbPhotos";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.FACEBOOK_LAYER;
	}
	
	@Override
	public boolean isCheckinable() {
    	return true;
    }
	
	@Override
	public boolean isPrimary() {
    	return false;
    }
	
	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Facebook_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.facebook_icon;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.facebook_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.facebook_128;
	}
}
