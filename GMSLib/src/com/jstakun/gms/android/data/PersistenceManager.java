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
public interface PersistenceManager {
    public static final String SEPARATOR_CHAR = ";";
    public static final String FORMAT = ".png";

    Bitmap readImageFile();
    Bitmap readImageFile(String filename);
    void saveImageFile(Bitmap map);
    void saveImageFile(Bitmap map, String filename);
    void saveLandmarkStore(List<ExtendedLandmark> landmarkdb);
    void saveConfigurationFile();
    int readLandmarkStore(List<ExtendedLandmark> landmarkdb);
    int readConfigurationFile();
    void saveTilesCache(TilesCache tilesCache);
    int readTilesCache(TilesCache tilesCache);
    void deleteFile();
    void deleteTile();
    boolean tileExists(String filename);
    int deleteTilesCache();
}
