/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.FacebookUtils;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.LoggerUtils;
import java.net.URLEncoder;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class FbCheckinsReader extends FbPlacesReader {

    @Override
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
                queryString += "&token=" + URLEncoder.encode(token, "UTF-8");

                url = ConfigurationManager.getInstance().getSecuredServicesUrl() + "fbCheckins?" + queryString;
                response = parser.parse(url, landmarks, Commons.FACEBOOK_LAYER, FBPLACES_PREFIX, -1, -1, task, false, limit);


                if (StringUtils.equals(response, FacebookUtils.FB_OAUTH_ERROR)) {
                    fbUtils.logout();
                }
            }
        } catch (Exception e) {
            LoggerUtils.error("FbCheckinsReader exception: ", e);
        } finally {
            close();
        }

        return response;
    }
}
