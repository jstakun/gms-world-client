package com.jstakun.gms.android.data;

import android.graphics.Bitmap;
import android.net.Uri;

import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.maps.TilesCache;
import java.util.List;

/**
 *
 * @author jstakun
 */
public interface PersistenceManager {
    public static final String SEPARATOR_CHAR = ";";
    public static final String FORMAT_PNG = ".png";
    public static final String FORMAT_LOG = ".log";

    Bitmap readImageFile();
    Bitmap readImageFile(String filename);
    void saveImageFile(Bitmap map);
    Uri saveImageFile(Bitmap map, String filename);
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
