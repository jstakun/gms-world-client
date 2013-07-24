/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.gms.android.maps.Tile;
import com.jstakun.gms.android.maps.TileFactory;
import com.jstakun.gms.android.maps.TilesCache;
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
    private static final String MAP_FILE = "lm_map" + FORMAT;
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

    public void saveImageFile(Bitmap map, String filename) {
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

    public void saveTilesCache(TilesCache tilesCache) {
        OutputStream ops = null;
        Context ctx = ConfigurationManager.getInstance().getContext();

        try {
            ops = ctx.openFileOutput(TILES_FILE, Context.MODE_PRIVATE);

            for (int i = 0; i < ConfigurationManager.getInstance().getInt(ConfigurationManager.SCREEN_SIZE); i++) {
                Tile tile = tilesCache.getTile(i);
                //save image
                saveImageFile(tile.getImage(), i + FORMAT);

                String line = Double.toString(tile.getLatitude()) + PersistenceManager.SEPARATOR_CHAR + Double.toString(tile.getLongtude()) + PersistenceManager.SEPARATOR_CHAR
                        + Integer.toString(tile.getXTile()) + PersistenceManager.SEPARATOR_CHAR + Integer.toString(tile.getYTile())
                        + PersistenceManager.SEPARATOR_CHAR + Integer.toString(tile.getZoom()) + "\n";

                //save tile data
                ops.write(line.getBytes());
            }
            ops.close();

        } catch (Exception ioe) {
            LoggerUtils.error("FileManager.saveTilesCache exception", ioe);
        } finally {
            if (ops != null) {
                try {
                    ops.close();
                } catch (IOException ex) {
                    LoggerUtils.debug("FileManager.saveTilesCache exception", ex);
                }

            }

        }
    }

    public int readTilesCache(TilesCache tilesCache) {
        Bitmap image = null;
        int result = 10;
        int i = 0;
        InputStream is = null;
        InputStreamReader isr = null;
        Context ctx = ConfigurationManager.getInstance().getContext();

        try {

            is = ctx.openFileInput(TILES_FILE);
            isr = new InputStreamReader(is, "UTF8");
            String line = null;

            while ((line = readLine(isr)) != null) {

                int separatorPos = line.indexOf(SEPARATOR_CHAR);

                //System.out.println("Record " + (i + 1) + ": " + line);

                if (separatorPos == -1) {
                	if (isr != null) {
                        try {
                            isr.close();
                        } catch (Exception ex) {
                            LoggerUtils.debug("FileManager.readTilesCache exception", ex);
                        }
                    }
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception ex) {
                            LoggerUtils.debug("FileManager.readTilesCache exception", ex);
                        }
                    }
                    throw new Exception("Separator character not found.");
                }

                double latitude = Double.parseDouble(line.substring(0, separatorPos).trim());
                int oldSeparatorPos = separatorPos;
                separatorPos = line.indexOf(SEPARATOR_CHAR, oldSeparatorPos + 1);
                double longitude = Double.parseDouble(line.substring(oldSeparatorPos + 1, separatorPos).trim());
                oldSeparatorPos = separatorPos;
                separatorPos = line.indexOf(SEPARATOR_CHAR, oldSeparatorPos + 1);
                int xtile = Integer.parseInt(line.substring(oldSeparatorPos + 1, separatorPos).trim());
                oldSeparatorPos = separatorPos;
                separatorPos = line.indexOf(SEPARATOR_CHAR, oldSeparatorPos + 1);
                int ytile = Integer.parseInt(line.substring(oldSeparatorPos + 1, separatorPos).trim());
                int zoom = Integer.parseInt(line.substring(separatorPos + 1).trim());

                image = readImageFile(i + FORMAT);

                if (image == null) {
                    return -1;
                }

                //Tile tile = new Tile(image, latitude, longitude, xtile, ytile, zoom, false);
                Tile tile = TileFactory.getTile(xtile, ytile, zoom, image);

                tilesCache.setTile(i, tile);
                i++;
            }

            result += i;

        } catch (Exception e) {
            LoggerUtils.error("FileManager.readTilesCache exception", e);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception ex) {
                    LoggerUtils.debug("FileManager.readTilesCache exception", ex);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                    LoggerUtils.debug("FileManager.readTilesCache exception", ex);
                }
            }
        }

        return result;
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
            if (fileList[i].endsWith(FORMAT)) {
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
