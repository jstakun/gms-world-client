/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

import java.util.List;

/**
 *
 * @author jstakun
 */
public class YelpReader extends AbstractJsonReader{
    @Override
    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        int dist = radius * 1000;
        String url = ConfigurationManager.getInstance().getServicesUrl() + "yelpProvider?lat=" + coords[0] + "&lng=" + coords[1] +
                "&radius=" + dist + "&limit=" + limit + "&display=" + display + "&version=2";
        return parser.parse(url, landmarks, Commons.YELP_LAYER, null, -1, -1, task, true, limit);
    }
}
