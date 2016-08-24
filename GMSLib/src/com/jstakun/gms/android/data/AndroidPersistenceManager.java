/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MercatorUtils;
import com.openlapi.QualifiedCoordinates;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class AndroidPersistenceManager implements PersistenceManager {

    private static final String CONFIGURATION_FILE = "lm_configuration";
    private static final String MAP_FILE = "lm_map" + FORMAT_PNG;
    private static final String TILES_FILE = "lm_tiles.txt";
    private static final String LANDMARKDB_FILE = "lm_landmarkdb.txt";

    public Bitmap readImageFile() {
        return readImageFile(MAP_FILE);
    }

    public Bitmap readImageFile(String filename) {
        Bitmap image = null;
        Context ctx = ConfigurationManager.getInstance().getContext();
        try {
            image = BitmapFactory.decodeStream(ctx.openFileInput(filename));
        } catch (Exception ex) {
            LoggerUtils.error("AndroidPersistenceManager.readImageFile exception", ex);
        }
        return image;
    }

    public void saveImageFile(Bitmap map) {
        saveImageFile(map, MAP_FILE);
    }

    public Uri saveImageFile(Bitmap map, String filename) {
        OutputStream out = null;
        Context ctx = ConfigurationManager.getInstance().getContext();

        try {
            out = ctx.openFileOutput(filename, Context.MODE_PRIVATE);
            map.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
        } catch (Exception ioe) {
            LoggerUtils.error("AndroidPersistenceManager.saveFile exception", ioe);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    LoggerUtils.debug("AndroidPersistenceManager.saveFile exception", ex);
                }
            }
        }
        return null;
    }

    public void saveLandmarkStore(List<ExtendedLandmark> landmarkdb) {
        Context ctx = ConfigurationManager.getInstance().getContext();
        OutputStream ops = null;
        Writer out = null;

        try {

            ops = ctx.openFileOutput(LANDMARKDB_FILE, Context.MODE_PRIVATE);
            out = new OutputStreamWriter(ops, "UTF8");

            for (int i = 0; i < landmarkdb.size(); i++) {
                ExtendedLandmark landmark = landmarkdb.get(i);

                String line = MercatorUtils.normalizeE6(landmark.getQualifiedCoordinates().getLatitude()) + SEPARATOR_CHAR
                        + MercatorUtils.normalizeE6(landmark.getQualifiedCoordinates().getLongitude()) + SEPARATOR_CHAR
                        + landmark.getName() + SEPARATOR_CHAR
                        + landmark.getDescription() + SEPARATOR_CHAR
                        + +landmark.getCreationDate() + "\n";
                out.write(line);
            }

        } catch (Exception ioe) {
            LoggerUtils.error("FileManager.saveLandmarkStore exception", ioe);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    LoggerUtils.debug("FileManager.saveLandmarkStore exception", ex);
                }
            }
            if (ops != null) {
                try {
                    ops.close();
                } catch (IOException ex) {
                    LoggerUtils.debug("FileManager.saveLandmarkStore exception", ex);
                }

            }
        }
    }

    public void saveConfigurationFile() {

        Context ctx = ConfigurationManager.getInstance().getContext();
        SharedPreferences settings = ctx.getSharedPreferences(CONFIGURATION_FILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        Map<String, String> config = ConfigurationManager.getInstance().getConfiguration();

        Set<String> keys = config.keySet();
        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            String value = config.get(key);
            editor.putString(key, value);
        }

        editor.commit();
    }

    public int readLandmarkStore(List<ExtendedLandmark> landmarkdb) {
        int result = -1;
        InputStream is = null;
        InputStreamReader isr = null;

        try {
            Context ctx = ConfigurationManager.getInstance().getContext();
            is = ctx.openFileInput(LANDMARKDB_FILE);
            isr = new InputStreamReader(is, "UTF8");
            String line = null;

            while ((line = readLine(isr)) != null) {
                String[] landmark = StringUtils.splitPreserveAllTokens(line, SEPARATOR_CHAR);

                    if (landmark.length == 5 || landmark.length == 4) {
                        String latitude = landmark[0];
                        String longitude = landmark[1];
                        String name = landmark[2];
                        String details = landmark[3];
                        long creationDate = System.currentTimeMillis();

                        if (landmark.length == 5) {
                            if (StringUtils.isNumeric(landmark[4])) {
                                creationDate = Long.parseLong(landmark[4]);
                            }
                        }

                        QualifiedCoordinates qc = new QualifiedCoordinates(Double.parseDouble(latitude), Double.parseDouble(longitude), 0f, Float.NaN, Float.NaN);
                        landmarkdb.add(LandmarkFactory.getLandmark(name, details, qc, Commons.LOCAL_LAYER, creationDate));
                    }
            }
            result = 0;

        } catch (Exception e) {
            LoggerUtils.error("FileManager.readLandmarkStore exception", e);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception ex) {
                    LoggerUtils.debug("FileManager.readLandmarkStore exception", ex);
                }

            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                    LoggerUtils.debug("FileManager.readLandmarkStore exception", ex);
                }

            }
        }

        return result;
    }

    public int readConfigurationFile() {
        Context ctx = ConfigurationManager.getInstance().getContext();
        SharedPreferences settings = ctx.getSharedPreferences(CONFIGURATION_FILE, 0);
        Map<String, ?> map = settings.getAll();
        for (String key : map.keySet()) {
            String value = (String) map.get(key);
            ConfigurationManager.getInstance().putString(key, value);
        }
        return 0;
    }

    public void deleteFile() {
        deleteFile(MAP_FILE);
    }

    private boolean deleteFile(String filename) {
        Context ctx = ConfigurationManager.getInstance().getContext();
        return ctx.deleteFile(filename);
    }

    public void deleteTile() {
        deleteFile(TILES_FILE);
    }

    public boolean tileExists(String filename) {
        Context ctx = ConfigurationManager.getInstance().getContext();
        String[] fileList = ctx.fileList();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].equals(filename)) {
                return true;
            }
        }

        return false;
    }

    public int deleteTilesCache() {
        int result = 0;
        Context ctx = ConfigurationManager.getInstance().getContext();
        String[] fileList = ctx.fileList();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].endsWith(FORMAT_PNG)) {
                ctx.deleteFile(fileList[i]);
                result++;
            }
        }
        return result;
    }

    private static String readLine(InputStreamReader reader) throws IOException {
        // Test whether the end of file has been reached. If so, return null.
        int readChar = reader.read();
        if (readChar == -1) {
            return null;
        }
        StringBuilder string = new StringBuilder("");
        while (readChar != -1 && readChar != '\n') {
            if (readChar != '\r') {
                string.append((char) readChar);
            }
            readChar = reader.read();
        }
        return string.toString();
    }
}
