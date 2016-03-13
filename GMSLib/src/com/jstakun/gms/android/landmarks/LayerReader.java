package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.utils.GMSAsyncTask;
import java.util.List;

/**
 *
 * @author jstakun
 */
public interface LayerReader {

    public abstract String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, int width, int height, String layer, GMSAsyncTask<?, ?, ?> task);

    public abstract void close();

    public abstract String[] getUrlPrefix();
}
