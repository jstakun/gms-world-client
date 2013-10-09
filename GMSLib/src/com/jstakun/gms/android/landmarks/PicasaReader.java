/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.BoundingBox;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.MercatorUtils;
import com.jstakun.gms.android.utils.StringUtil;
import java.util.List;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author jstakun
 */
public class PicasaReader extends AbstractSerialReader {

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task) {
		BoundingBox bbox = MercatorUtils.getBoundingBox(width, height, latitude, longitude, zoom);
		params.add(new BasicNameValuePair("bbox", StringUtil.formatCoordE2(bbox.west) + "," + StringUtil.formatCoordE2(bbox.south) + "," +
                StringUtil.formatCoordE2(bbox.east) + "," + StringUtil.formatCoordE2(bbox.north)));
		String url = ConfigurationManager.getInstance().getServicesUrl() + "picasaProvider?" + URLEncodedUtils.format(params, "UTF-8");
		
		return parser.parse(url, landmarks, task, true, null);
	}

    /*public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        BoundingBox bbox = MercatorUtils.getBoundingBox(width, height, latitude, longitude, zoom);

        String url = ConfigurationManager.getInstance().getServicesUrl() + "picasaProvider?bbox=" +
                StringUtil.formatCoordE2(bbox.west) + "," + StringUtil.formatCoordE2(bbox.south) + "," +
                StringUtil.formatCoordE2(bbox.east) + "," + StringUtil.formatCoordE2(bbox.north) + "&version=4" + "&limit=" + limit + "&display=" + display;

        //System.out.println(url);

        return parser.parse(url, landmarks, Commons.PICASA_LAYER, null, -1, -1, task, true, limit);
    }*/
}
