/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

import java.util.List;
import java.util.Locale;

/**
 *
 * @author jstakun
 */
public class HotelsReader extends AbstractSerialReader {

	@Override
	protected String readLayer(List<ExtendedLandmark> landmarks,
			double latitude, double longitude, int zoom, int width, int height,
			String layer, GMSAsyncTask<?, ?, ?> task) {
		CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm == null || cm.isCategoryEnabled(CategoriesManager.CATEGORY_TRAVEL)) {
            String l = Locale.getDefault().getLanguage();
            String url = ConfigurationManager.getInstance().getServicesUrl() + "hotelsProvider?"
                    + "latitudeMin=" + coords[0] + "&longitudeMin=" + coords[1] + "&radius=" + radius
                    + "&lang=" + l + "&limit=" + limit + "&version=" + AbstractSerialReader.SERIAL_VERSION + "&display=" + display + "&format=bin";
            return parser.parse(url, landmarks, task, true, null);
        } else {
            return null;
        }
	}

    //private static final String[] prefixes = new String[] {"http://www.hotelscombined.com/Hotel/"};
    /*public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm == null || cm.isCategoryEnabled(CategoriesManager.CATEGORY_TRAVEL)) {
            
            //if (OsUtil.isIceCreamSandwichOrHigher()) {
            //    limit = 100;
            //}

            String l = Locale.getDefault().getLanguage();

            String url = ConfigurationManager.getInstance().getServicesUrl() + "hotelsProvider?"
                    + "latitudeMin=" + coords[0] + "&longitudeMin=" + coords[1] + "&radius=" + radius
                    + "&lang=" + l + "&limit=" + limit + "&version=3" + "&display=" + display;

            return parser.parse(url, landmarks, Commons.HOTELS_LAYER, null, CategoriesManager.CATEGORY_TRAVEL, CategoriesManager.SUBCATEGORY_HOTEL, task, true, limit);
        } else {
            return null;
        }
    }*/
}
