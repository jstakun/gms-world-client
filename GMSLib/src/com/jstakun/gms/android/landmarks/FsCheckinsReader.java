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
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author jstakun
 */
public class FsCheckinsReader extends AbstractSerialReader {

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		String l = Locale.getDefault().getLanguage();
        String errorMessage = null;

        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
            ISocialUtils fsUtils = OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE);
            String token = fsUtils.getAccessToken().getToken();
            if (token != null) {
               String query_string = "lat=" + coords[0] + "&lng=" + coords[1] + "&radius=" + radius + "&lang=" + l + "&limit=" + limit + "&display=" + display + "&format=bin&version=3";

               try {
                  String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "fsCheckins?" + query_string + "&token=" + URLEncoder.encode(token, "UTF-8");
                  errorMessage = parser.parse(url, landmarks, task, true, Commons.FOURSQUARE, 1);
               } catch (Exception e) {
                  errorMessage = e.getMessage();
                  LoggerUtils.error("FsCheckinsReader exception: ", e);
               }
            } else {
            	LoggerUtils.error("FsCheckinsReader exception: token is null");
            }
        }
        
        return errorMessage;
	}

    /*@Override
    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        String l = Locale.getDefault().getLanguage();
        String errorMessage = null;

        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
            ISocialUtils fsUtils = OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE);
            String token = fsUtils.getAccessToken().getToken();
            if (token != null) {
               String query_string = "lat=" + coords[0] + "&lng=" + coords[1] + "&radius=" + radius + "&lang=" + l + "&limit=" + limit + "&display=" + display + "&version=3";

               try {
                  String url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "fsCheckins?" + query_string + "&token=" + URLEncoder.encode(token, "UTF-8");
                  errorMessage = parser.parse(url, landmarks, Commons.FOURSQUARE_LAYER, FOURSQUARE_PREFIX, -1, -1, task, false, limit);
               } catch (Exception e) {
                  errorMessage = e.getMessage();
                  LoggerUtils.error("FsCheckinsReader exception: ", e);
               }
            } else {
            	LoggerUtils.error("FsCheckinsReader exception: token is null");
            }
        }

        close();

        return errorMessage;
    }*/
}
