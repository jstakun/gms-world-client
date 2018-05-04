package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import java.util.List;

/**
 *
 * @author jstakun
 */
public interface LayerReader {

    public abstract String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, String layer, GMSAsyncTask<?, ?, ?> task);

    public abstract void close();

    public abstract String[] getUrlPrefix();
    
    public abstract String getLayerName(boolean formatted);
    
    public abstract int getDescriptionResource();
    
    public abstract int getSmallIconResource();
    
    public abstract int getLargeIconResource();
    
    public abstract int getImageResource();
    
    public abstract boolean isEnabled(); 
    
    public abstract boolean isCheckinable();
    
    public abstract boolean isPrimary();
    
    public abstract FileManager.ClearPolicy getClearPolicy();
    
    public abstract int getPriority();
}
