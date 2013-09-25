/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class CouponsReader extends AbstractSerialReader {

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks,
			double latitude, double longitude, int zoom, int width, int height,
			String layer, GMSAsyncTask<?, ?, ?> task) {
		String url = ConfigurationManager.getInstance().getServicesUrl() + 
		            "couponsProvider?latitude=" + coords[0] + "&longitude=" + coords[1] + "&radius=" + radius + 
                    "&version=" + SERIAL_VERSION + "&dealLimit=" + dealLimit + "&display=" + display + "&format=bin";
        
        CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm != null) {
            String categoryid = cm.getEnabledCategoriesString();
            if (StringUtils.isNotEmpty(categoryid)) {
                url += "&categoryid=" + categoryid;
            }
        }

        return parser.parse(url, landmarks, task, true, null, SERIAL_VERSION);
	}

    /*@Override
    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        String url = ConfigurationManager.getInstance().getServicesUrl() + "couponsProvider?latitude=" + coords[0] + 
                "&longitude=" + coords[1] + "&radius=" + radius + "&version=4" + "&dealLimit=" + dealLimit + "&display=" + display;
        
        CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm != null) {
            String categoryid = cm.getEnabledCategoriesString();
            if (StringUtils.isNotEmpty(categoryid)) {
                url += "&categoryid=" + categoryid;
            }
        }

        return parser.parse(url, landmarks, Commons.COUPONS_LAYER, null, -1, -1, task, true, 10);
    }*/
}
