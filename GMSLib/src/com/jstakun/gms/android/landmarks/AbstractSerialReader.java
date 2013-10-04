package com.jstakun.gms.android.landmarks;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.StringUtil;

public abstract class AbstractSerialReader implements LayerReader {
	private static final int DEFAULT_LIMIT = 30;
    private static final int DEFAULT_DEAL_LIMIT = 300;
    protected SerialParser parser = null;
    //protected String[] coords = null;
    protected int radius; //, limit = DEFAULT_LIMIT, dealLimit = DEFAULT_DEAL_LIMIT;
    //protected String display = null;
	private static final String SERIAL_VERSION = "11";
	protected List<NameValuePair> params = new ArrayList<NameValuePair>();
     
    private void init(double latitude, double longitude, int zoom, int width, int height) {
        parser = new SerialParser();
        //coords = new String[] {StringUtil.formatCoordE6(latitude), StringUtil.formatCoordE6(longitude)};
        //display = OsUtil.getDisplayType();
        radius = DistanceUtils.radiusInKilometer();
        int limit = ConfigurationManager.getInstance().getInt(ConfigurationManager.LANDMARKS_PER_LAYER, DEFAULT_LIMIT);
        if (limit == DEFAULT_LIMIT && OsUtil.isIceCreamSandwichOrHigher()) {
            limit = 2 * limit;
        } 
        int dealLimit = ConfigurationManager.getInstance().getInt(ConfigurationManager.DEAL_LIMIT, DEFAULT_DEAL_LIMIT);      
        if (dealLimit == DEFAULT_DEAL_LIMIT && OsUtil.isIceCreamSandwichOrHigher()) {
            dealLimit = (int) (1.5 * dealLimit);
        }
        params.add(new BasicNameValuePair("radius", Integer.toString(radius)));
        params.add(new BasicNameValuePair("latitude", StringUtil.formatCoordE6(latitude)));
        params.add(new BasicNameValuePair("longitude", StringUtil.formatCoordE6(longitude)));
        params.add(new BasicNameValuePair("limit", Integer.toString(limit)));
        params.add(new BasicNameValuePair("dealLimit", Integer.toString(dealLimit)));
        params.add(new BasicNameValuePair("display", OsUtil.getDisplayType()));
        params.add(new BasicNameValuePair("format", "bin"));
        params.add(new BasicNameValuePair("version", SERIAL_VERSION));
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
