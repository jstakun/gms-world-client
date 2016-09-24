package com.jstakun.gms.android.landmarks;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.deals.CategoriesManager;

/**
 *
 * @author jstakun
 */
public class GrouponReader extends AbstractSerialReader {

   	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
   		int dist = radius * 4;
		params.put("radius", Integer.toString(dist));
	    String categoryid = CategoriesManager.getInstance().getEnabledCategoriesString();
        if (StringUtils.isNotEmpty(categoryid)) {
           params.put("categoryid", categoryid);
        }
    }

	@Override
	protected String getUri() {
		return "grouponProvider";
	}
}
