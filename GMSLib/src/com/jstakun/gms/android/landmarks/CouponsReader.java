/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import org.apache.commons.lang.StringUtils;
import org.apache.http.message.BasicNameValuePair;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;

/**
 *
 * @author jstakun
 */
public class CouponsReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
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
		return ConfigurationManager.getInstance().getServicesUrl() + "couponsProvider";
	}
}
