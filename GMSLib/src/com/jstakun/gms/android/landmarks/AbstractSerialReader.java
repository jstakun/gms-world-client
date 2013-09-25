package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.StringUtil;

public abstract class AbstractSerialReader implements LayerReader {
	private static final int DEFAULT_LIMIT = 30;
    private static final int DEFAULT_DEAL_LIMIT = 300;
    protected static final int SERIAL_VERSION = 5; //2
    
    protected SerialParser parser = null;
    protected String[] coords = null;
    protected int radius, limit = DEFAULT_LIMIT, dealLimit = DEFAULT_DEAL_LIMIT;
    protected String display = null;
     
    private void init(double latitude, double longitude, int zoom, int width, int height) {
        parser = new SerialParser();
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
    
    protected abstract String readLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task);

    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
    	init(latitude, longitude, zoom, width, height);
    	return readLayer(landmarks, latitude, longitude, zoom, width, height, layer, task);
    }

    public void close() {
        parser.close();
    }

    public String[] getUrlPrefix() {
        return null;
    }
}
