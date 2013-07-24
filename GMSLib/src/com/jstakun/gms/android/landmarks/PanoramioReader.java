/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.MercatorUtils;
import com.jstakun.gms.android.utils.StringUtil;
import java.util.List;

/**
 *
 * @author jstakun
 */

//http://localhost:8080/panoramio2Provider?minx=20.957803&miny=52.239617&maxx=20.968103&maxy=52.24802

public class PanoramioReader extends AbstractJsonReader {

    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {

        init(latitude, longitude, zoom, width, height);
        BoundingBox bbox = MercatorUtils.getBoundingBox(width, height, latitude, longitude, zoom);
        
        String panoramioURL = ConfigurationManager.getInstance().getServicesUrl() + "panoramio2Provider?" +
        "minx=" + StringUtil.formatCoordE2(bbox.west) + "&miny=" + StringUtil.formatCoordE2(bbox.south) + "&maxx=" +
        StringUtil.formatCoordE2(bbox.east) + "&maxy=" + StringUtil.formatCoordE2(bbox.north) + "&limit=" + limit + "&display=" + display + "&version=3";

        //System.out.println(panoramioURL);

        return parser.parse(panoramioURL, landmarks, Commons.PANORAMIO_LAYER, null, -1, -1, task, true, limit);
    }
}
