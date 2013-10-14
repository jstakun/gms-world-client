/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicNameValuePair;

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

    private boolean hasToken = false;
	
	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		params.add(new BasicNameValuePair("lang", Locale.getDefault().getLanguage()));
		
		CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm != null) {
            String categoryid = cm.getEnabledCategoriesString();
            if (StringUtils.isNotEmpty(categoryid)) {
            	params.add(new BasicNameValuePair("&categoryid", categoryid));
            }
        }
        
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
			ISocialUtils fsUtils = OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE);
			String token = fsUtils.getAccessToken().getToken();
			if (token != null) {
				params.add(new BasicNameValuePair("token", token));
				hasToken = true;
			}	
		}
	}
	
	@Override
	protected String getUrl() {
		if (hasToken) {
			return  ConfigurationManager.getInstance().getSecuredServicesUrl() + "foursquareMerchant";
		} else {
			return ConfigurationManager.SERVER_URL + "foursquareMerchant";
		}
	}
}
