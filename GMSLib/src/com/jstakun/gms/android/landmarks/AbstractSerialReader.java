package com.jstakun.gms.android.landmarks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.StringUtil;

public abstract class AbstractSerialReader implements LayerReader {
	private static final int DEFAULT_LIMIT = 30;
    private static final int DEFAULT_DEAL_LIMIT = 300;
    private static final String SERIAL_VERSION = "12";
    protected Map<String, String> params = null;
    protected SerialParser parser = null;
    protected int radius;
     
    protected void init(double latitude, double longitude, int zoom, int width, int height) {
        parser = new SerialParser();
        radius = DistanceUtils.radiusInKilometer();
        int limit = ConfigurationManager.getInstance().getInt(ConfigurationManager.LANDMARKS_PER_LAYER, DEFAULT_LIMIT);
        if (limit == DEFAULT_LIMIT && OsUtil.isIceCreamSandwichOrHigher()) {
            limit = 2 * limit;
        } 
        int dealLimit = ConfigurationManager.getInstance().getInt(ConfigurationManager.DEAL_LIMIT, DEFAULT_DEAL_LIMIT);      
        if (dealLimit == DEFAULT_DEAL_LIMIT && OsUtil.isIceCreamSandwichOrHigher()) {
            dealLimit = (int) (1.5 * dealLimit);
        }
        params = new HashMap<String, String>();
        params.put("radius", Integer.toString(radius));
        params.put("latitude", StringUtil.formatCoordE6(latitude));
        params.put("longitude", StringUtil.formatCoordE6(longitude));
        params.put("limit", Integer.toString(limit));
        params.put("dealLimit", Integer.toString(dealLimit));
        params.put("display", OsUtil.getDisplayType());
        params.put("format", "bin");
        params.put("version", SERIAL_VERSION);
    }
    
    protected String[] getUrls() { 
    	List<String> urls = new ArrayList<String>(3);
    	if (ConfigurationManager.getUserManager().isTokenPresent()) {
    		//urls.add(ConfigurationManager.getInstance().getSecuredServicesUrl() + getUri());
    		urls.add(ConfigurationManager.getInstance().getSecuredRHCloudUrl() + getUri());
    	} 
    	urls.add(ConfigurationManager.getInstance().getAnonymousServerUrl() + getUri());
    	return urls.toArray(new String[urls.size()]);
    }

    public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ? ,?> task) {
    	init(latitude, longitude, zoom, width, height);
    	params.put("layer", layer);
	    return parser.parse(getUrls(), 0, params, landmarks, task, true, layer, false);
    }

    public final void close() {
    	//LoggerUtils.debug("Closing layer reader for " + getUrl());
    	//long start = System.currentTimeMillis();
    	parser.close();
        //LoggerUtils.debug("Closed layer reader for " + getUrl() + " in " + (System.currentTimeMillis()-start) + " millis" );
    }

    public final String[] getUrlPrefix() {
        return null;
    }
    
    public boolean isEnabled() {
    	return true;
    }
    
    protected abstract String getUri();
    
    public boolean isCheckinable() {
    	return false;
    }
    
    public boolean isPrimary() {
    	return true;
    }
    
    public FileManager.ClearPolicy getClearPolicy() {
    	return FileManager.ClearPolicy.ONE_MONTH; //this is legacy code replaced by picasso library
    }
    
    public int getPriority() {
    	return 100;
    }
}
