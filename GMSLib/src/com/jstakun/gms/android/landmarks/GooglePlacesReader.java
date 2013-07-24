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
public class GooglePlacesReader extends AbstractJsonReader {

    @Override
    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        String lang = Locale.getDefault().getLanguage();
        String url = ConfigurationManager.getInstance().getServicesUrl() + "googlePlacesProvider?latitude=" + coords[0] + 
                "&longitude=" + coords[1] + "&radius=" + (radius*1000) + "&language=" + lang + "&limit=" + limit + "&display=" + display + "&version=2";
        return parser.parse(url, landmarks, Commons.GOOGLE_PLACES_LAYER, null, -1, -1, task, true, limit);
    }
}
