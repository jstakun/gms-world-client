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
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class GrouponReader extends AbstractJsonReader {

    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
        init(latitude, longitude, zoom, width, height);
        String url = ConfigurationManager.getInstance().getServicesUrl() + "grouponProvider?lat=" + coords[0] + 
                "&lng=" + coords[1] + "&radius=" + (radius * 4) + "&version=4" + "&dealLimit=" + dealLimit + "&display=" + display;
        CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm != null) {
            String categoryid = cm.getEnabledCategoriesString();
            if (StringUtils.isNotEmpty(categoryid)) {
                url += "&categoryid=" + categoryid;
            }
        }
        return parser.parse(url, landmarks, Commons.GROUPON_LAYER, null, -1, -1, task, true, 10);
    }
}
