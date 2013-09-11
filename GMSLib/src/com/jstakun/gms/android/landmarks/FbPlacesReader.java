/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import java.net.URLEncoder;
import java.util.List;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 *
 * @author jstakun
 */
public class FbPlacesReader extends AbstractSerialReader {

    public static final String[] FBPLACES_PREFIX = {"http://touch.facebook.com/profile.php?id="};

    /*public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {

        String url, response = null;

        try {
            init(latitude, longitude, zoom, width, height);

            int dist = radius;
            if (dist > 6371) {
                dist = 6371;
            }
            
            String queryString = "lat=" + coords[0] + "&lng=" + coords[1] + "&distance=" +
                    dist + "&limit=" + limit + "&display=" + display + "&version=2";

            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
                ISocialUtils fbUtils = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK);
                String token = fbUtils.getAccessToken().getToken();
                url = ConfigurationManager.getInstance().getSecuredServicesUrl() +
                        "facebookProvider?" + queryString + "&token=" + URLEncoder.encode(token, "UTF-8");
            } else {
                url = ConfigurationManager.SERVER_URL + "facebookProvider?" + queryString;
            }

            response = parser.parse(url, landmarks, Commons.FACEBOOK_LAYER, FBPLACES_PREFIX, -1, -1, task, true, limit);

            if (StringUtils.equals(response, FacebookUtils.FB_OAUTH_ERROR)) {
                if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
                    ISocialUtils fbUtils = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK);
                    fbUtils.logout();
                }

                //call again without token
                url = ConfigurationManager.SERVER_URL + "facebookProvider?" + queryString;
                response = parser.parse(url, landmarks, Commons.FACEBOOK_LAYER, FBPLACES_PREFIX, -1, -1, task, true, limit);
            }
        } catch (Exception e) {
            LoggerUtils.error("FBPlacesReader exception: ", e);
        } finally {
            close();
        }

        return response;
    }*/

    @Override
    public String[] getUrlPrefix() {
        return FBPLACES_PREFIX;
    }

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		String url = null, response = null;

		int dist = radius;
        if (dist > 6371) {
            dist = 6371;
        }
        
        String queryString = "lat=" + coords[0] + "&lng=" + coords[1] + "&distance=" +
                dist + "&limit=" + limit + "&display=" + display + "&version=2&format=bin";

        try {
        	if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
        		ISocialUtils fbUtils = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK);
        		String token = fbUtils.getAccessToken().getToken();
        		url = ConfigurationManager.getInstance().getSecuredServicesUrl() +
                    "facebookProvider?" + queryString + "&token=" + URLEncoder.encode(token, "UTF-8");
        	} else {
        		url = ConfigurationManager.SERVER_URL + "facebookProvider?" + queryString;
        	}
        	response = parser.parse(url, landmarks, task, true, Commons.FACEBOOK);
        } catch (Exception e) {
            LoggerUtils.error("FBPlacesReader.readLayer() exception: ", e);
        }	

        return response;
	}
}
