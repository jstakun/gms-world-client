package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;

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
    
    @Override
    public String getLayerName(boolean formatted) {
		return Commons.FACEBOOK_LAYER;
	}
    
    @Override
	public boolean isCheckinable() {
    	return true;
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
	
	@Override
	public int getPriority() {
		return 2;
	}
}
