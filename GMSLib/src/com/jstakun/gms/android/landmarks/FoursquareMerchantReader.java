/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class FoursquareMerchantReader extends AbstractSerialReader {

    private static final String[] FOURSQUARE_PREFIX = {"http://foursquare.com/mobile/venue/"};

    /*public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        try {
            init(latitude, longitude, zoom, width, height);
            String l = Locale.getDefault().getLanguage();
            String url = null;
            String query_string = "lat=" + coords[0] + "&lng=" + coords[1] + 
                    "&radius=" + radius + "&lang=" + l + "&limit=" + limit + "&display=" + display + "&version=3";

            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
                ISocialUtils fsUtils = OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE);
                String token = fsUtils.getAccessToken().getToken();
                if (token != null) {
                	url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "foursquareMerchant?" + query_string + "&token=" + URLEncoder.encode(token, "UTF-8");
                } else {
                	LoggerUtils.error("FoursquareMerchantReader exception: token is null!");
                    url = ConfigurationManager.SERVER_URL + "foursquareMerchant?" + query_string;
                }
            } else {
                url = ConfigurationManager.SERVER_URL + "foursquareMerchant?" + query_string;
            }

            CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
            if (cm != null) {
                String categoryid = cm.getEnabledCategoriesString();
                if (StringUtils.isNotEmpty(categoryid)) {
                    url += "&categoryid=" + categoryid;
                }
            }

            return parser.parse(url, landmarks, Commons.FOURSQUARE_MERCHANT_LAYER, FOURSQUARE_PREFIX, -1, -1, task, true, limit);

        } catch (Exception e) {
            LoggerUtils.error("FoursquareMerchantReader exception: ", e);
        }
        return null;
    }*/
    
    

    @Override
    public String[] getUrlPrefix() {
        return FOURSQUARE_PREFIX;
    }

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		try {
            String l = Locale.getDefault().getLanguage();
            String url = null;
            String query_string = "lat=" + coords[0] + "&lng=" + coords[1] + 
                    "&radius=" + radius + "&lang=" + l + "&limit=" + limit + "&display=" + display + "&version=3&format=bin";

            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
                ISocialUtils fsUtils = OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE);
                String token = fsUtils.getAccessToken().getToken();
                if (token != null) {
                	url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "foursquareMerchant?" + query_string + "&token=" + URLEncoder.encode(token, "UTF-8");
                } else {
                	LoggerUtils.error("FoursquareMerchantReader.readLayer() exception: token is null!");
                    url = ConfigurationManager.SERVER_URL + "foursquareMerchant?" + query_string;
                }
            } else {
                url = ConfigurationManager.SERVER_URL + "foursquareMerchant?" + query_string;
            }

            CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
            if (cm != null) {
                String categoryid = cm.getEnabledCategoriesString();
                if (StringUtils.isNotEmpty(categoryid)) {
                    url += "&categoryid=" + categoryid;
                }
            }

            return parser.parse(url, landmarks, task, true, Commons.FOURSQUARE);

        } catch (Exception e) {
            LoggerUtils.error("FoursquareMerchantReader.readLayer() exception: ", e);
        }
        return null;
	}


}
