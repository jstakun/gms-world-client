/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.MercatorUtils;
import com.jstakun.gms.android.utils.StringUtil;

/**
 *
 * @author jstakun
 */
public class OsmReader extends AbstractJsonReader {

    @Override
    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        BoundingBox bbox = MercatorUtils.getBoundingBox(width, height, latitude, longitude, zoom);
        
        String amenity = "atm"; //LayerManager.OSM_ATM_LAYER
        if (layer.equals(Commons.OSM_PARKING_LAYER)){
            amenity = "parking";
        }

        String url = ConfigurationManager.getInstance().getServicesUrl() + "osmProvider?" +
                     "latitudeMin=" + StringUtil.formatCoordE2(bbox.south) + "&latitudeMax=" + StringUtil.formatCoordE2(bbox.north) +
                     "&longitudeMin=" + StringUtil.formatCoordE2(bbox.west) + "&longitudeMax=" + StringUtil.formatCoordE2(bbox.east) +
                     "&amenity=" + amenity + "&limit=" + limit + "&display=" + display;

        return parser.parse(url, landmarks, layer, null , -1, -1, task, true, limit);
    }
}
