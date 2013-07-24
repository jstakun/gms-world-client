/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

import java.util.List;
import java.util.Locale;

/**
 *
 * @author jstakun
 */
public class FoursquareReader extends AbstractJsonReader {

    protected static final String[] FOURSQUARE_PREFIX = {"http://foursquare.com/mobile/venue/"};

    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        String l = Locale.getDefault().getLanguage();
        String url = ConfigurationManager.getInstance().getServicesUrl() + "foursquareProvider?lat=" + coords[0] + "&lng=" + coords[1]
                + "&radius=" + radius + "&lang=" + l + "&version=3" + "&limit=" + limit;

        String errorMessage = parser.parse(url, landmarks, Commons.FOURSQUARE_LAYER, FOURSQUARE_PREFIX, -1, -1, task, true, limit);

        close();

        return errorMessage;
    }

    @Override
    public String[] getUrlPrefix() {
        return FOURSQUARE_PREFIX;
    }
}
