/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.MercatorUtils;
import java.util.List;

/**
 *
 * @author jstakun
 */
//http://api.eventful.com/rest/events/search?location=32.746682,-117.162741&within=0.5&date=Future&app_key=key&units=km&page_size=100

public class EventfulReader extends AbstractJsonReader {

    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {

        init(latitude, longitude, zoom, width, height);

        double[] a = MercatorUtils.adjust(latitude, longitude, -width / 2, height / 2, zoom);
        double[] b = MercatorUtils.adjust(latitude, longitude, width / 2, -height / 2, zoom);

        double within = MercatorUtils.normalizeE6(MathUtils.abs(a[0]-b[0]) * 10.0);
        //double within = 1.0;

        String url = ConfigurationManager.getInstance().getServicesUrl() + "eventfulProvider?" +
                     "location=" + coords[0] + "," + coords[1] + "&within=" + within +
                     "&date=Future&units=km&format=json&page_size=" + limit + "&version=4" + "&display=" + display;

        return parser.parse(url, landmarks, Commons.EVENTFUL_LAYER, null, -1, -1, task, true, limit);
    }
}
