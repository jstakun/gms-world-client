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
import com.jstakun.gms.android.utils.LoggerUtils;
import java.util.List;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author jstakun
 */
public class FbCheckinsReader extends AbstractSerialReader {

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks,
			double latitude, double longitude, int zoom, int width, int height,
			String layer, GMSAsyncTask<?, ?, ?> task) {
		
		String response = null;

        try {
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {

                //int dist = radius;
                //if (dist > 6371) {
                //    dist = 6371;
                //}
                //params.add(new BasicNameValuePair("distance", Integer.toString(dist)));

                //String queryString = "lat=" + coords[0] + "&lng=" + coords[1] + "&distance="
                //        + dist + "&limit=" + limit + "&display=" + display + "&version=" + SERIAL_VERSION + "&format=bin";

                ISocialUtils fbUtils = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK);
                String token = fbUtils.getAccessToken().getToken();
                if (token != null) {
                	params.add(new BasicNameValuePair("token", token));
                	String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "fbCheckins?" + URLEncodedUtils.format(params, "UTF-8");
                	response = parser.parse(url, landmarks, task, true, Commons.FACEBOOK);
                } else {
                	LoggerUtils.error("FbCheckinsReader.readLayer() exception: token is null");
                }
            }
        } catch (Exception e) {
            LoggerUtils.error("FbCheckinsReader.readLayer() exception: ", e);
        } 

        return response;
	}

    /*@Override
    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {

        String url, response = null;

        try {
            init(latitude, longitude, zoom, width, height);
            if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {

                int dist = radius;
                if (dist > 6371) {
                    dist = 6371;
                }

                String queryString = "lat=" + coords[0] + "&lng=" + coords[1] + "&distance="
                        + dist + "&limit=" + limit + "&display=" + display + "&version=2";

                ISocialUtils fbUtils = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK);
                String token = fbUtils.getAccessToken().getToken();
                if (token != null) {
                	queryString += "&token=" + URLEncoder.encode(token, "UTF-8");

                	url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "fbCheckins?" + queryString;
                	response = parser.parse(url, landmarks, Commons.FACEBOOK_LAYER, FBPLACES_PREFIX, -1, -1, task, false, limit);

                	if (StringUtils.equals(response, FacebookUtils.FB_OAUTH_ERROR)) {
                		fbUtils.logout();
                	}
                } else {
                	LoggerUtils.error("FbCheckinsReader exception: token is null");
                }
            }
        } catch (Exception e) {
            LoggerUtils.error("FbCheckinsReader exception: ", e);
        } finally {
            close();
        }

        return response;
    }*/
}
