package com.jstakun.gms.android.landmarks;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;

/**
 *
 * @author jstakun
 */
public class FoursquareMerchantReader extends AbstractSerialReader {

    @Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		params.put("lang", Locale.getDefault().getLanguage());
		
		String categoryid = CategoriesManager.getInstance().getEnabledCategoriesString();
        if (StringUtils.isNotEmpty(categoryid)) {
            params.put("&categoryid", categoryid);
        }
        
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
			ISocialUtils fsUtils = OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE);
			String token = fsUtils.getAccessToken().getToken();
			if (token != null) {
				params.put("token", token);
			}	
		}
	}
	
	@Override
	protected String getUri() {
		return "foursquareMerchant";		
	}
}
