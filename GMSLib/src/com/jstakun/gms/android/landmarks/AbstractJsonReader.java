/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.StringUtil;
import java.util.List;

/**
 *
 * @author jstakun
 */
public abstract class AbstractJsonReader implements LayerReader {

    private static final int DEFAULT_LIMIT = 30;
    private static final int DEFAULT_DEAL_LIMIT = 300;
    
    protected JSONParser parser;
    protected String[] coords;
    protected int radius, limit = DEFAULT_LIMIT, dealLimit = DEFAULT_DEAL_LIMIT;
    protected String display;
     
    protected void init(double latitude, double longitude, int zoom, int width, int height) {
        parser = new JSONParser();
        coords = new String[] {StringUtil.formatCoordE6(latitude), StringUtil.formatCoordE6(longitude)};
        radius = DistanceUtils.radiusInKilometer();
        display = OsUtil.getDisplayType();
        limit = ConfigurationManager.getInstance().getInt(ConfigurationManager.LANDMARKS_PER_LAYER, DEFAULT_LIMIT);
        if (limit == DEFAULT_LIMIT && OsUtil.isIceCreamSandwichOrHigher()) {
            limit = 2 * limit;
        } 
        dealLimit = ConfigurationManager.getInstance().getInt(ConfigurationManager.DEAL_LIMIT, DEFAULT_DEAL_LIMIT);      
        if (dealLimit == DEFAULT_DEAL_LIMIT && OsUtil.isIceCreamSandwichOrHigher()) {
            dealLimit = (int) (1.5 * dealLimit);
        }
    }

    public abstract String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task);

    public void close() {
        parser.close();
    }

    public String[] getUrlPrefix() {
        return null;
    }
}
