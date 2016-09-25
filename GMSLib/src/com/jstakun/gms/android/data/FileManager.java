package com.jstakun.gms.android.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.landmarks.LandmarkParcelableFactory;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MercatorUtils;
import com.openlapi.QualifiedCoordinates;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.DisplayMetrics;

/**
 *
 * @author jstakun
 */
public class FileManager implements PersistenceManager {

    private static final String MAP_FILE = "lm_map" + FORMAT_PNG;
    public static final String CONFIGURATION_FILE = "lm_configuration.txt";
    public static final String LANDMARKDB_FILE = "lm_landmarkdb.txt";
    private static final String IMAGES_FOLDER = "Images";
    private static final String TILES_FOLDER = "Tiles";
    private static final String ROUTES_FOLDER = "Routes";
    private static final String FILES_FOLDER = "Files";
    private static final String ICONS_FOLDER = "Icons";
    private static final String LOGS_FOLDER = "Logs";
    private static final String ROOT_FOLDER_PREFIX = "/Android/data/";
    private static final String TIMESTAMP = "timestamp";
    
    protected static FileManager instance = new FileManager();
    
    public enum ClearPolicy {ONE_DAY, ONE_WEEK, ONE_MONTH, ONE_QUARTER, ONE_YEAR}; 

    public static FileManager getInstance() {
    	return instance;
    }
    
    private FileManager() {
    }

    public Bitmap readImageFile() {
        return readImageFile(getImagesFolder(), MAP_FILE, null);
    }

    public Bitmap readImageFile(String filename) {
        return readImageFile(getImagesFolder(), filename, null);
    }

    public Bitmap readImageFile(String folder, String filename, DisplayMetrics displayMetrics) {
        File fc;
        Bitmap image = null;

        try {
            fc = getExternalDirectory(folder, filename);
            if (!fc.exists()) {
                LoggerUtils.debug("No saved file at " + fc.getAbsolutePath());
            } else {
            	fc.setLastModified(System.currentTimeMillis());
                Bitmap b = BitmapFactory.decodeStream(new FileInputStream(fc));
                if (b != null && !b.isRecycled()) {
                    if (displayMetrics != null) {
                        int newWidth = (int) (b.getWidth() * displayMetrics.density);
                        int newHeight = (int) (b.getHeight() * displayMetrics.density);
                        image = Bitmap.createScaledBitmap(b, newWidth, newHeight, true);
                        if (image != b) {
                        	b.recycle();
                        }
                    } else {
                        image = b;
                    }
                }
            }
        } catch (Exception ex) {
            LoggerUtils.error("FileManager.readImageFile() exception", ex);
        }
        return image;
    }

    public void saveImageFile(Bitmap map) {
        saveImageFile(map, MAP_FILE);
    }

    protected Bitmap readImageFileFromCache(String filename, DisplayMetrics displayMetrics) {
        File fc;
        Bitmap image = null;

        try {
            fc = new File(ConfigurationManager.getInstance().getContext().getCacheDir(), filename);
            if (!fc.exists()) {
                LoggerUtils.debug("File " + fc.getAbsolutePath() + " doesn't exists...");
            } else {
            	fc.setLastModified(System.currentTimeMillis());
                Bitmap b = BitmapFactory.decodeStream(new FileInputStream(fc));
                if (b != null && !b.isRecycled()) {
                    if (displayMetrics != null) {
                        int newWidth = (int) (b.getWidth() * displayMetrics.density);
                        int newHeight = (int) (b.getHeight() * displayMetrics.density);
                        image = Bitmap.createScaledBitmap(b, newWidth, newHeight, true);
                        if (image != b) {
                        	b.recycle();
                        }
                    } else {
                        image = b;
                    }
                }
            }
        } catch (Exception ex) {
            LoggerUtils.error("FileManager.readImageFile exception", ex);
        }
        return image;
    }

    public void saveFile(byte[] buffer, String directory, String filename) {

        File fc;
        OutputStream out = null;
        try {
            fc = getExternalDirectory(directory, filename);
            out = new FileOutputStream(fc);
            out.write(buffer);
            out.flush();
        } catch (Exception ioe) {
            LoggerUtils.error("FileManager.saveFile exception", ioe);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    LoggerUtils.debug("FileManager.saveFile exception", ex);
                }
            }
        }
    }

    public Uri saveImageFile(Bitmap image, String filename) {
        return saveImageFile(image, getImagesFolder(), filename);
    }

    public Uri saveIconFile(Bitmap map, String filename) {
        return saveImageFile(map, getIconsFolderPath(), filename);
    }

    public void saveImageFileToCache(Bitmap map, String filename, boolean compress) {
        if (map != null) {
        	OutputStream out = null;
        	try {
        		File fc = new File(ConfigurationManager.getInstance().getContext().getCacheDir(), filename);
        		out = new FileOutputStream(fc);
        		if (compress) {
        			map.compress(Bitmap.CompressFormat.PNG, 100, out);
        		}
        		out.flush();
        	} catch (Exception ioe) {
        		LoggerUtils.error("FileManager.saveFile exception", ioe);
        	} finally {
        		if (out != null) {
        			try {
        				out.close();
        			} catch (IOException ex) {
        				LoggerUtils.debug("FileManager.saveFile exception", ex);
        			}
        		}
        	}
        } else {
        	LoggerUtils.debug("Bitamp " + filename + " is empty!");
        }
    }

    private Uri saveImageFile(Bitmap map, String folder, String filename) {
        File fc = null;
        OutputStream out = null;
        try {
            fc = getExternalDirectory(folder, filename);
            out = new FileOutputStream(fc);
            map.compress(Bitmap.CompressFormat.PNG, 100, out);
            //map.compress(Bitmap.CompressFormat.JPEG, 50, out);
            out.flush();
        } catch (Exception ioe) {
            LoggerUtils.error("FileManager.saveFile exception", ioe);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    LoggerUtils.debug("FileManager.saveFile exception", ex);
                }
            }
        }
        if (fc != null) {
        	return Uri.fromFile(fc);
        } else {
        	return null;
        }
    }

    private String readLine(InputStreamReader reader) throws IOException {
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

    public void saveLandmarkStore(List<ExtendedLandmark> landmarkdb) {
        File fc, outf;
        OutputStream ops = null;
        Writer out = null;

        try {
            fc = getExternalDirectory(null, LANDMARKDB_FILE + ".tmp");
            outf = getExternalDirectory(null, LANDMARKDB_FILE);
            if (!fc.exists()) {
                fc.createNewFile();
            }
            ops = new FileOutputStream(fc);
            out = new OutputStreamWriter(ops, "UTF8");

            for (int i = 0; i < landmarkdb.size(); i++) {
                ExtendedLandmark landmark = landmarkdb.get(i);

                String line = MercatorUtils.normalizeE6(landmark.getQualifiedCoordinates().getLatitude()) + SEPARATOR_CHAR
                        + MercatorUtils.normalizeE6(landmark.getQualifiedCoordinates().getLongitude()) + SEPARATOR_CHAR
                        + landmark.getName() + SEPARATOR_CHAR
                        + landmark.getDescription() + SEPARATOR_CHAR
                        + landmark.getCreationDate() + "\n";
                out.write(line);
                out.flush();
            }

            fc.renameTo(outf);

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

        File fc, out;

        OutputStream ops = null;

        try {
            LoggerUtils.debug("Saving file " + CONFIGURATION_FILE);
            fc = getExternalDirectory(null, CONFIGURATION_FILE + ".tmp");
            out = getExternalDirectory(null, CONFIGURATION_FILE);
            if (!fc.exists()) {
                fc.createNewFile();
            }
            ops = new FileOutputStream(fc);
            Map<String, String> config = ConfigurationManager.getInstance().getConfiguration();

            for (Iterator<Map.Entry<String, String>> i = config.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, String> entry = i.next();
                String key = entry.getKey();
                String line = key + SEPARATOR_CHAR + entry.getValue() + "\n";
                ops.write(line.getBytes());
                ops.flush();
            }

            String line = TIMESTAMP + SEPARATOR_CHAR + System.currentTimeMillis() + "\n";
            ops.write(line.getBytes());
            ops.flush();

            fc.renameTo(out);

        } catch (Exception ioe) {
            LoggerUtils.error("FileManager.saveConfigurationFile exception", ioe);
        } finally {
            if (ops != null) {
                try {
                    ops.close();
                } catch (IOException ex) {
                    LoggerUtils.debug("FileManager.saveConfigurationStore exception", ex);
                }

            }
        }
    }

    public int readLandmarkStore(List<ExtendedLandmark> vector) {
        File fc;
        int result = -1;
        InputStream is = null;
        InputStreamReader isr = null;

        try {
            fc = getExternalDirectory(null, LANDMARKDB_FILE);
            if (fc.exists() && fc.length() > 0) {
                is = new FileInputStream(fc);
                isr = new InputStreamReader(is, "UTF8");

                String line;
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
                        vector.add(LandmarkFactory.getLandmark(name, details, qc, Commons.LOCAL_LAYER, creationDate));
                    }
                }
                result = 0;
            }
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
        File fc;
        int result = -1;
        InputStream is = null;
        InputStreamReader isr = null;
        Map<String, String> config = new HashMap<String, String>();

        try {
            LoggerUtils.debug("Opening file " + CONFIGURATION_FILE);

            fc = getExternalDirectory(null, CONFIGURATION_FILE);
            if (fc.exists()) {
                is = new FileInputStream(fc);
                isr = new InputStreamReader(is, "UTF8");
                String line;
                while ((line = readLine(isr)) != null) {
                    if (!line.startsWith("#")) {
                        int separatorPos = line.indexOf(SEPARATOR_CHAR);
                        if (separatorPos == -1) {
                            throw new Exception("Separator character not found.");
                        }
                        String key = line.substring(0, separatorPos);
                        String value = line.substring(separatorPos + 1);
                        config.put(key, value);
                    }
                }

                if (config.containsKey(TIMESTAMP)) {
                    config.remove(TIMESTAMP);
                    ConfigurationManager.getInstance().putAll(config);
                    result = config.size();
                }
            }
        } catch (Exception e) {
            LoggerUtils.error("FileManager.readConfigurationFile exception", e);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception ex) {
                    LoggerUtils.debug("FileManager.readConfigurationFile exception", ex);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                    LoggerUtils.debug("FileManager.readConfigurationFile exception", ex);
                }
            }
        }

        return result;
    }

    public String readTextFile(int resname, Context context) {
        //LoggerUtils.debug("Loading resource bundle from file " + filename);
        InputStream is = null;
        String result = "";

        try {
            is = context.getResources().openRawResource(resname);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int ch;
            while ((ch = is.read()) != -1) {
                baos.write(ch);
            }
            byte[] textData = baos.toByteArray();
            baos.close();
            result = new String(textData);
        } catch (Exception e) {
            LoggerUtils.error("FileManager.readTextFile exception", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                    LoggerUtils.error("FileManager.readRTextFile exception", ex);
                }
            }
        }
        return result;
    }

    public int readResourceBundleFile(Map<String, String> config, int resname, Context context) {
        //LoggerUtils.debug("Loading resource bundle from file " + filename);
        InputStream is = null;
        InputStreamReader isr = null;
        int result = -1;

        try {
            is = context.getResources().openRawResource(resname);
            isr = new InputStreamReader(is, "UTF8");
            
            String line;
            while ((line = readLine(isr)) != null) {
                if (!line.startsWith("#")) {
                    int separatorPos = line.indexOf(SEPARATOR_CHAR);
                    if (separatorPos == -1) {
                    	if (isr != null) {
                            try {
                                isr.close();
                            } catch (Exception ex) {
                                LoggerUtils.error("FileManager.readResourceBundleFile exception", ex);
                            }
                        }
                        if (is != null) {
                            try {
                                is.close();
                            } catch (Exception ex) {
                                LoggerUtils.error("FileManager.readResourceBundleFile() exception", ex);
                            }
                        }
                        throw new Exception("Separator character not found.");
                    }
                    String key = line.substring(0, separatorPos);
                    String value = line.substring(separatorPos + 1);
                    //LoggerUtils.debug("Loading setting " + key + ": " + value);
                    config.put(key, value);
                }
            }
            result = 0;
        } catch (Exception e) {
            LoggerUtils.error("FileManager.readResourceBundleFile() exception", e);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception ex) {
                    LoggerUtils.error("FileManager.readResourceBundleFile() exception", ex);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                    LoggerUtils.error("FileManager.readResourceBundleFile() exception", ex);
                }
            }
        }
        return result;
    }

    public String readJsonFile(int resname, Context context) {
        //LoggerUtils.debug("Loading resource bundle from file " + filename);
        InputStream is = null;
        InputStreamReader isr = null;
        String json = "";

        try {
            is = context.getResources().openRawResource(resname);
            isr = new InputStreamReader(is, "UTF8");

            String line;
            while ((line = readLine(isr)) != null) {
                json += line;
            }
        } catch (Exception e) {
            LoggerUtils.error("FileManager.readResourceBundleFile exception", e);
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (Exception ex) {
                    LoggerUtils.error("FileManager.readResourceBundleFile exception", ex);
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                    LoggerUtils.error("FileManager.readResourceBundleFile exception", ex);
                }
            }
        }
        return json.trim();
    }

    public String getRoutesFolderPath() {
        createFolder(ROUTES_FOLDER);
    	return ROUTES_FOLDER;
    }

    public String getFileFolderPath() {
    	createFolder(FILES_FOLDER);
        return FILES_FOLDER;
    }

    public String getIconsFolderPath() {
    	createFolder(ICONS_FOLDER);
        return ICONS_FOLDER;
    }

    private String getImagesFolder() {
    	createFolder(IMAGES_FOLDER);
        return IMAGES_FOLDER;
    }

    public String getTilesFolder() {
    	createFolder(TILES_FOLDER);
        return TILES_FOLDER;
    }
    
    private String getLogsFolder() {
    	createFolder(LOGS_FOLDER);
        return LOGS_FOLDER;
    }

    private boolean createFolder(String path) {
    	File fc;
    	boolean res = false;
    	
    	try {
    		fc = getExternalDirectory(path, null);
            if (!fc.exists()) {
                res = fc.mkdirs();
                LoggerUtils.debug("Created routes folder: " + res);
            }
    	} catch (Exception ex) {
    		LoggerUtils.error("FileManager.createFolder() exception", ex);
        }
    	return res;
    }    

    public void deleteFile() {
        deleteFile(getImagesFolder(), MAP_FILE);
    }

    public void deleteRouteFile(String filename) {
        deleteFile(getRoutesFolderPath(), filename);
    }

    public void deletePoiFile(String filename) {
        deleteFile(getFileFolderPath(), filename);
    }

    public void deleteFile(String folder, String filename) {
        try {
            File fc = getExternalDirectory(folder, filename);
            if (fc != null) {
                fc.delete();
            }
        } catch (Exception ex) {
            LoggerUtils.error("FileManager.deleteFile() exception", ex);
        }
    }
    
    public void renameRouteFile(String oldName, String newName) {
        renameFile(getRoutesFolderPath(), oldName, newName);
    }

    public void renamePoiFile(String oldName, String newName) {
        renameFile(getFileFolderPath(), oldName, newName);
    }
    
    private void renameFile(String folder, String oldName, String newName) {
    	try {
            File oldFile = getExternalDirectory(folder, oldName);
            File newFile = getExternalDirectory(folder, newName);
            oldFile.renameTo(newFile);
        } catch (Exception ex) {
            LoggerUtils.error("FileManager.renameFile() exception", ex);
        }
    }

    public void clearImageCache() {
        new ClearCacheTask().execute(ConfigurationManager.getInstance().getContext().getCacheDir());
    }

    public boolean fileExists(String path, String filename) {
        File fc;
        boolean result = false;
        try {
            fc = getExternalDirectory(path, filename);
            if (fc.exists()) {
                result = true;
            }
        } catch (Exception ex) {
            LoggerUtils.error("FileManager.fileExists() exception", ex);
        }

        return result;
    }

    public List<String> readFolder(String path, FilenameFilter filter) {
        File fc;
        List<String> files = new ArrayList<String>();
        try {
            fc = getExternalDirectory(path, null);
            if (fc.isDirectory()) {
                File[] fileList = fc.listFiles(filter);
                if (fileList != null) {
                	java.util.Locale currentLocale = ConfigurationManager.getInstance().getCurrentLocale();
                	for (int i = 0; i < fileList.length; i++) {
                		File f = fileList[i];
                		files.add(f.getName());
                		String length = Formatter.formatFileSize(ConfigurationManager.getInstance().getContext(), f.length());
                		String date = DateTimeUtils.getShortDateTimeString(f.lastModified(), currentLocale);
                		files.add(length + " | " + date);
                	}
                }
            }
        } catch (Exception ex) {
            LoggerUtils.error("FileManager.readFolder() exception", ex);
        }

        return files;
    }

    public ArrayList<LandmarkParcelable> readFolderAsLandmarkParcelable(String path, FilenameFilter filter, String layer) {
        File fc;
        ArrayList<LandmarkParcelable> files = new ArrayList<LandmarkParcelable>();
        try {
            fc = getExternalDirectory(path, null);
            if (fc.isDirectory()) {
                //apply filefilter
                File[] fileList = fc.listFiles(filter);
                Function<File, LandmarkParcelable> transformFunction = new FileToLandmarkParcelableFunction(layer);
                files.addAll(Lists.transform(Arrays.asList(fileList), transformFunction));
            }
        } catch (Exception ex) {
            LoggerUtils.error("FileManager.readFolderAsLandmarkParcelable() exception", ex);
        }

        return files;
    }
    
    public boolean isFolderEmpty(String path, FilenameFilter filter) {
    	try {
            File fc = getExternalDirectory(path, null);
            if (fc.exists() && fc.isDirectory()) {
                //apply filefilter
                String[] fileList = fc.list(filter);
                return (fileList == null || fileList.length == 0);
            }
        } catch (Exception ex) {
            LoggerUtils.error("FileManager.isFolderEmpty() exception", ex);
        }
    	return true;
    }

    public File getExternalDirectory(String path, String file) {

        String absolutePath = ROOT_FOLDER_PREFIX + ConfigurationManager.getAppUtils().getPackageInfo().packageName + "/files";

        if (path != null) {
            absolutePath += "/" + path;
        }

        if (file != null) {
            absolutePath += "/" + file;
        }

        return new File(Environment.getExternalStorageDirectory() + absolutePath);
    }

    public File getRouteFile(String filename) {
        return getExternalDirectory(getRoutesFolderPath(), filename);
    }
    
    public File getPoiFile(String filename) {
        return getExternalDirectory(getFileFolderPath(), filename);
    }

    public Writer openKmlRouteFile(File fc) throws IOException {
        Writer out = new OutputStreamWriter(new FileOutputStream(fc), "UTF8");

        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
        out.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\r\n");
        out.write(" <Document>\r\n");
        out.write("  <name>Landmark Manager Trail</name>\r\n");
        out.write("  <Folder>\r\n");
        out.write("   <name>Trails</name>\r\n");
        out.flush();

        return out;
    }

    public void closeKmlRouteFile(Writer out) throws IOException {
        if (out != null) {
            out.write("  </Folder>\r\n");
            out.write("</Document>\r\n");
            out.write("</kml>");
            out.flush();
        }
    }

    public void saveKmlRoute(List<ExtendedLandmark> landmarkList, String description, String filename) {

        File fc;
        OutputStream ops = null;
        Writer out = null;
        try {
            fc = getExternalDirectory(getRoutesFolderPath(), filename);
            ops = new FileOutputStream(fc);
            out = new OutputStreamWriter(ops, "UTF8");

            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
            out.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\r\n");
            out.write(" <Document>\r\n");
            out.write("  <name>Landmark Manager Route</name>\r\n");
            out.write("  <description>" + description + "</description>\r\n");
            out.write("  <Folder>\r\n");
            out.write("   <name>Route</name>\r\n");
            out.flush();

            if (!landmarkList.isEmpty()) {
                ExtendedLandmark landmark = landmarkList.get(0);

                writePlacemark(landmark, out);

                out.write("   <Placemark>\r\n");
                out.write("    <name>" + landmark.getName() + "</name>\r\n");
                out.write("     <MultiGeometry>\r\n");
                out.write("      <LineString>\r\n");
                out.write("       <coordinates>");
                out.flush();

                int size = landmarkList.size() - 1;
                for (int i = 1; i < size; i++) {
                    landmark = landmarkList.get(i);
                    double coords[] = MercatorUtils.normalizeE6(landmark.getQualifiedCoordinates().getLongitude(), landmark.getQualifiedCoordinates().getLatitude());
                    Float altitude = landmark.getQualifiedCoordinates().getAltitude();
                    if (altitude.isNaN()) {
                        altitude = 0.0f;
                    }
                    out.write(coords[0] + "," + coords[1] + "," + altitude);
                    if (i < size-1) {
                        out.write(" ");
                    }
                }
                out.flush();

                out.write("</coordinates>\r\n");
                out.write("      </LineString>\r\n");
                out.write("     </MultiGeometry>\r\n");
                out.write("   </Placemark>\r\n");
                out.flush();

                writePlacemark(landmarkList.get(size), out);
            }

            out.write("  </Folder>\r\n");
            out.write("</Document>\r\n");
            out.write("</kml>");
            out.flush();

        } catch (Exception ioe) {
            LoggerUtils.error("FileManager.saveKMLRoute exception", ioe);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    LoggerUtils.debug("FileManager.saveKMLRoute exception", ex);
                }
            }
            if (ops != null) {
                try {
                    ops.close();
                } catch (IOException ex) {
                    LoggerUtils.debug("FileManager.saveKMLRoute exception", ex);
                }

            }
        }
    }

    public void writePlacemark(ExtendedLandmark landmark, Writer out) throws IOException {
        if (out != null && landmark != null) {
            out.write("   <Placemark>\r\n");
            out.write("    <name>" + landmark.getName() + "</name>\r\n");
            out.write("   <description>" + landmark.getDescription() + "</description>\r\n");
            //out.write("    <description>" + "" + "</description>\r\n");
            //<IconStyle>
            //<Icon>
            //<href>http://maps.google.com/mapfiles/kml/pal3/icon61.png</href>
            //</Icon>
            //</IconStyle>
            out.write("   <Point>\r\n");
            out.write("    <coordinates>" + landmark.getQualifiedCoordinates().getLongitude() + "," + landmark.getQualifiedCoordinates().getLatitude() + ",0</coordinates>\r\n");
            out.write("   </Point>\r\n");
            out.write("  </Placemark>\r\n");
            out.flush();
        }
    }

    private class ClearCacheTask extends GMSAsyncTask<File, Void, Void> {

    	public ClearCacheTask() {
        	super(10, ClearCacheTask.class.getName());
        }

        @Override
        protected Void doInBackground(File... params) {
        	//delete old images
        	deleteFiles(params[0], new FileDeletePredicate());
        	//delete old logs
        	deleteFiles(getExternalDirectory(getLogsFolder(), null), new FileDeletePredicate());
        	//save log file in debug mode
        	LoggerUtils.saveLogcat(Environment.getExternalStorageDirectory() + ROOT_FOLDER_PREFIX + ConfigurationManager.getAppUtils().getPackageInfo().packageName + "/files/" + LOGS_FOLDER + "/logcat" + System.currentTimeMillis() + ".txt");       	
        	//set context == null
        	LoggerUtils.debug("Deleting saved application context.");
        	ConfigurationManager.getInstance().setContext(null);
        	return null;
        }
        
        private int deleteFiles(File dir, FileDeletePredicate fp) {
        	int count = 0;
        	int total = 0; 
        	File[] fileList = null;
        	if (dir != null) {
        		fileList = dir.listFiles();
        	}
        	if (fileList != null) {
        		total = fileList.length;
        		LoggerUtils.debug("Found " + total +  " files in " + dir.getAbsolutePath());
        		for (File f : Iterables.filter(Arrays.asList(fileList), fp)) {
        			if (f.isDirectory()) {
        				count += deleteFiles(f, fp);
        			} 
        			//LoggerUtils.debug("Deleting file " + f.getAbsolutePath() + " created on " + DateTimeUtils.getShortDateTimeString(f.lastModified(), Locale.US));
        			f.delete();
        			count++;
        		}
        	}
        	if (dir != null) {
        		LoggerUtils.debug("Deleted " + count + " files from "  + dir.getAbsolutePath());
        	}
        	return count;
        }
    }
    
    private class FileDeletePredicate implements Predicate<File> {

    	@Override
		public boolean apply(File file) {
			String name = file.getName();
			String[] tokens = StringUtils.split(name, "_");
			ClearPolicy policy = null;
			
			if (tokens.length > 1) {
				String layerName = tokens[tokens.length-1];
				//LoggerUtils.debug("Checking clear policy for " + file.getName());
				policy = LayerManager.getInstance().getClearPolicy(layerName);
			} else {
				//LoggerUtils.debug("Applying default clear policy for " + file.getName());
			}
			
			if (policy == null) {
				policy = ClearPolicy.ONE_MONTH;
			}
			
			long interval = -1;
			
			switch (policy) { 
				case ONE_DAY:
					interval = System.currentTimeMillis() - DateTimeUtils.ONE_DAY;
					break;
				case ONE_WEEK:
					interval = System.currentTimeMillis() - DateTimeUtils.ONE_WEEK;
					break;
				case ONE_QUARTER: 
					interval = System.currentTimeMillis() - DateTimeUtils.ONE_QUATER;
					break;
				case ONE_YEAR:
					interval = System.currentTimeMillis() - DateTimeUtils.ONE_YEAR;
					break;
				default: 
					//ONE_MONTH
					interval = System.currentTimeMillis() - DateTimeUtils.ONE_MONTH;
					break;
			 }
			
             if (file.lastModified() < interval) {
            	 return true;
			 } else {
				 return false;
			 }
		}    	
    }
    
    private class FileToLandmarkParcelableFunction implements Function<File, LandmarkParcelable> {

    	private int pos = -1;
    	private String layer = null;
    	private java.util.Locale locale;
    	
    	public FileToLandmarkParcelableFunction(String layer) {
    		this.layer = layer;
    		this.locale = ConfigurationManager.getInstance().getCurrentLocale();
    	}
    	
		@Override
		public LandmarkParcelable apply(File f) {
			pos++;
			return LandmarkParcelableFactory.getLandmarkParcelable(f, pos, layer, locale);
		}
    	
    }
}
