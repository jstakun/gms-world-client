package com.jstakun.gms.android.data;

import java.util.List;

import com.jstakun.gms.android.landmarks.ExtendedLandmark;

import android.graphics.Bitmap;
import android.net.Uri;

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
    void deleteFile();
    void deleteTile();
    boolean tileExists(String filename);
    int deleteTilesCache();
}
