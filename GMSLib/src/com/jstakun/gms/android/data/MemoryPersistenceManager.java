package com.jstakun.gms.android.data;

import android.graphics.Bitmap;
import android.net.Uri;

import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class MemoryPersistenceManager implements PersistenceManager {

    public Bitmap readImageFile() {
        return null;
    }

    public void saveImageFile(Bitmap map) {
        //can't persist anything
    }

    public void saveLandmarkStore(List<ExtendedLandmark> landmarkdb) {
        //can't persist anything
    }

    public void saveConfigurationFile() {
         //can't persist anything
    }

    public int readLandmarkStore(List<ExtendedLandmark> landmarks) {
        return 0;
    }

    public int readConfigurationFile() {
        //ConfigurationManager.getInstance().setDefaultConfiguration();
        return 0;
    }

    public void deleteFile() {

    }

    public Bitmap readImageFile(String filename) {
        return null;
    }

    public Uri saveImageFile(Bitmap map, String filename) {
    	return null;
    }

}
