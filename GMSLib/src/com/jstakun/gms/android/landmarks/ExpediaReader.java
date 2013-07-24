/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;

import java.util.List;

/**
 *
 * @author jstakun
 */
public class ExpediaReader extends AbstractJsonReader {

    @Override
    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm == null || cm.isCategoryEnabled(CategoriesManager.CATEGORY_TRAVEL)) {
            init(latitude, longitude, zoom, width, height);
            
            String url = ConfigurationManager.getInstance().getServicesUrl() + "expediaProvider?latitude=" + coords[0] +
                    "&longitude=" + coords[1] + "&radius=" + radius + "&limit=" + limit + "&display=" + display;
            return parser.parse(url, landmarks, Commons.EXPEDIA_LAYER, null, CategoriesManager.CATEGORY_TRAVEL, CategoriesManager.SUBCATEGORY_HOTEL, task, true, limit);
        } else {
            return null;
        }
    }
}
