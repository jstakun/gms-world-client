/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author jstakun
 */
public class GrouponReader extends AbstractSerialReader {

   	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
   		int dist = radius * 4;
		params.remove(0); //remove default radius parameter
		params.add(new BasicNameValuePair("radius", Integer.toString(dist)));
	    CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        if (cm != null) {
            String categoryid = cm.getEnabledCategoriesString();
            if (StringUtils.isNotEmpty(categoryid)) {
            	params.add(new BasicNameValuePair("categoryid", categoryid));
            }
        }
    }

	@Override
	protected String getUrl() {
		return ConfigurationManager.getInstance().getServerUrl() + "grouponProvider";
	}
}
