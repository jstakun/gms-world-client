/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.data;

import android.graphics.Bitmap;

import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.maps.TilesCache;
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

    public void saveTilesCache(TilesCache tilesCache) {
         //can't persist anything
    }

    public int readTilesCache(TilesCache tilesCache) {
        return 0;
    }

    public void deleteFile() {

    }

    public void deleteTile() {
        
    }

    public boolean tileExists(String filename) {
        return false;
    }

    public int deleteTilesCache() {
        return 0;
    }

    public Bitmap readImageFile(String filename) {
        return null;
    }

    public void saveImageFile(Bitmap map, String filename) {
    }

}
