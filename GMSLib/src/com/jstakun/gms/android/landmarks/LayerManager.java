package com.jstakun.gms.android.landmarks;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.deals.Category;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import dalvik.system.DexFile;

/**
 *
 * @author jstakun
 */
public class LayerManager {

	public static final int LAYER_LOCAL = 1;
    public static final int LAYER_EXTERNAL = 2;
    public static final int LAYER_DYNAMIC = 0;
    public static final int LAYER_FILESYSTEM = 3;
    public static final int LAYER_ICON_SMALL = 0;
    public static final int LAYER_ICON_LARGE = 1;
    
    private static final Map<String, Layer> layers = new LinkedHashMap<String, Layer>();
    private static final Map<String, Layer> allLayers = new LinkedHashMap<String, Layer>();
    private static final List<String> dynamicLayers = new ArrayList<String>();
    
    private static boolean initialized = false;
    
    private static LayerManager instance = new LayerManager();

    public static LayerManager getInstance() {
    	return instance;
    }
    
    private LayerManager() {
    	
    	Map<String, List<LayerReader>> readers = new HashMap<String, List<LayerReader>>();
    	
    	//1. load all layer reader grouped by layer
    	try {
    		String[] classes = getClassesOfPackage("com.jstakun.gms.android.landmarks");
    		LoggerUtils.debug("Found " + classes.length + " classes in package com.jstakun.gms.android.landmarks");
    		for (String className : classes) {
    			Class<?> clazz = Class.forName(className);
    			if (LayerReader.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
    				LayerReader reader = (LayerReader)clazz.newInstance();
    				if (reader.isEnabled()) {
    					LoggerUtils.debug("Layer reader " + className + " is enabled");
    					String layerName = reader.getLayerName(false);
    					if (readers.containsKey(layerName)) {
    						readers.get(layerName).add(reader);
    					} else {
    						List<LayerReader> layerReaders = new ArrayList<LayerReader>();
    						layerReaders.add(reader);
    						readers.put(layerName, layerReaders);
    					}
    				} else {
    					LoggerUtils.debug("Layer reader " + className + " is disabled");
    				}
    			} else {
    				LoggerUtils.debug("Class " + className + " is not layer reader");
    			}
    		}
    	} catch (Exception e) {
    		LoggerUtils.error(e.getMessage(), e);
    	} finally {
    		LoggerUtils.debug("Done");
    	}
    	
    	//2. sort layers by priority and add to allLayers
    	List<List<LayerReader>> entries = new ArrayList<List<LayerReader>>(); 
    	entries.addAll(readers.values());
    	Collections.sort(entries, new LayerReaderComparator());

		//3. copy sorted layers to allLayers collection		
		for (List<LayerReader> layerReaders : entries) {
			LayerReader layerReader = layerReaders.get(0);
			if (layerReaders.size() > 1) {
				for (LayerReader lr : layerReaders) {
					if (lr.isPrimary()) {
						layerReader = lr;
						break;
					}
				}
			}
			String layerName = layerReader.getLayerName(false);
			LoggerUtils.debug("Adding layer " + layerName + " with " + layerReaders.size() + " reader(s)");
			allLayers.put(layerName, 
					LayerFactory.getLayer(layerName, true, isLayerEnabledConf(layerName), layerReader.isCheckinable(),
							layerReaders, null, layerReader.getSmallIconResource(), null, layerReader.getLargeIconResource(), LAYER_LOCAL, 
							Locale.getMessage(layerReader.getDescriptionResource()), layerReader.getLayerName(true), layerReader.getClearPolicy(), layerReader.getImageResource()));
		}

        //add this two layers manually
        
        allLayers.put(Commons.ROUTES_LAYER, LayerFactory.getLayer(Commons.ROUTES_LAYER, true, isLayerEnabledConf(Commons.ROUTES_LAYER), false, null, null, R.drawable.route, null, R.drawable.start_marker, LAYER_LOCAL, Locale.getMessage(R.string.Layer_Routes_desc), Commons.ROUTES_LAYER, FileManager.ClearPolicy.ONE_MONTH, R.drawable.routes_128));
        allLayers.put(Commons.MY_POSITION_LAYER, LayerFactory.getLayer(Commons.MY_POSITION_LAYER, true, isLayerEnabledConf(Commons.MY_POSITION_LAYER), false, null, null, R.drawable.mypos16, null, R.drawable.ic_maps_indicator_current_position, LAYER_LOCAL, null, Commons.MY_POSITION_LAYER, FileManager.ClearPolicy.ONE_MONTH, 0));
    }

    public boolean isLayerEnabled(String layerName) {
        //System.out.println("Calling IsLayerEnabled with param " + name);
        Layer layer = layers.get(layerName);
        if (layer != null) {
            return layer.isEnabled();
        } else {
            return false;
        }
    }

    public boolean isLayerCheckinable(String layerName) {
        //System.out.println("Calling IsLayerEnabled with param " + name);
        Layer layer = layers.get(layerName);
        if (layer != null) {
            return layer.isCheckinable();
        } else {
            return false;
        }
    }

    public void setLayerEnabled(String name, boolean enabled) {
        Layer layer = layers.get(name);
        if (layer != null) {
            layer.setEnabled(enabled);
        }
    }

    public void enableAllLayers() {
        for (String key: getLayers()) {
            setLayerEnabled(key, true);
        }
    }
    
    public boolean isAllLayersEnabled() {
        for (String key : getLayers()) {
            Layer layer = layers.get(key);
            if (layer != null && (!layer.isEnabled() && layer.getType() != LAYER_DYNAMIC)) {
                return false;
            }
        }
        return true;
    }
    
    public void disableAllLayers() {
        for (String key : getLayers()) {
        	setLayerEnabled(key, false);
        }
    }

    public boolean isEmpty() {
        return layers.isEmpty();
    }

    public Layer getLayer(String key) {
        return layers.get(key);
    }
    
    public boolean containsLayer(String key) {
    	return layers.containsKey(key);
    }

    public List<String> getLayers() {
        if (initialized) {
            synchronized (layers) {
                return new ArrayList<String>(layers.keySet());
            }
        } else {
            return new ArrayList<String>();
        }
    }

    public Map<String, String> getExternalLayers() {
        Map<String, String> extLayers = new LinkedHashMap<String, String>();
        List<String> excluded = Arrays.asList(new String[]{"Social", "Geocodes"});

        synchronized (layers) {
            for (Iterator<String> iter = Iterables.filter(layers.keySet(), new LayerExternalNotInListPredicate(excluded)).iterator(); iter.hasNext();) {
                Layer layer = layers.get(iter.next());
                extLayers.put(layer.getName(), layer.getFormatted());
            }
        }

        return Collections.unmodifiableMap(extLayers);
    }

    public List<String> getEnabledLayers() {
        List<String> enabled = new ArrayList<String>();
        synchronized (layers) {
            for (Iterator<Map.Entry<String, Layer>> i = layers.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, Layer> entry = i.next();
                Layer layer = entry.getValue();

                if (layer.isEnabled() && layer.getType() != LAYER_DYNAMIC) {
                    enabled.add(entry.getKey());
                }
            }
        }

        return enabled;
    }

    protected int getLoadableLayersCount() {
        int counter = 0;
        for (String key : getLayers()) {
            Layer layer = layers.get(key);
            if (layer != null) {
                int size = layer.getLayerReader().size();
                counter += size;
            }
        }
        return counter;
    }

    private boolean isLayerEnabledConf(String layerName) {
        boolean enabled = true;
        String key = layerName + "_status";

        if (ConfigurationManager.getInstance().isOff(key)) {
            enabled = false;
        }

        return enabled;
    }

    protected void initialize(String... layerNames) {
        synchronized (layers) {
            layers.clear();
            dynamicLayers.clear();
            initializeDynamicLayers();

            if (layerNames.length == 0) {
                for (Iterator<String> iter = Iterables.filter(allLayers.keySet(), new LayerEnabledPredicate()).iterator(); iter.hasNext();) {
                    String layer = iter.next();
                    layers.put(layer, allLayers.get(layer));
                }
                for (Iterator<String> iter = Iterables.filter(allLayers.keySet(), new LayerDisabledPredicate()).iterator(); iter.hasNext();) {
                    String layer = iter.next();
                    layers.put(layer, allLayers.get(layer));
                }
            } else {
                List<String> layerNamesList = Arrays.asList(layerNames);
                for (Iterator<String> iter = Iterables.filter(allLayers.keySet(), new LayerEnabledInListPredicate(layerNamesList)).iterator(); iter.hasNext();) {
                    String layer = iter.next();
                    layers.put(layer, allLayers.get(layer));
                }
                for (Iterator<String> iter = Iterables.filter(allLayers.keySet(), new LayerDisabledInListPredicate(layerNamesList)).iterator(); iter.hasNext();) {
                    String layer = iter.next();
                    layers.put(layer, allLayers.get(layer));
                }
            }
            initialized = true;
        }
    }

    protected void addLayer(String name, boolean extensible, boolean manageable, boolean enabled, boolean checkinable, boolean searchable, String smallIconPath, String largeIconPath, String desc, String formatted) {
        synchronized (layers) {
            if (smallIconPath == null) {
                layers.put(name, LayerFactory.getLayer(name, manageable, enabled, checkinable, null, null, R.drawable.custom, null, -1, LAYER_LOCAL, desc, formatted, FileManager.ClearPolicy.ONE_MONTH, 0));
            } else {
                layers.put(name, LayerFactory.getLayer(name, manageable, enabled, checkinable, null, smallIconPath, -1, largeIconPath, -1, LAYER_FILESYSTEM, desc, formatted, FileManager.ClearPolicy.ONE_MONTH, 0));
            }
        }
    }

    public void removeLayer(String name) {
        synchronized (layers) {
            if (layers.containsKey(name)) {
                layers.remove(name);
            }
        }
    }

    protected final String initializeExternalLayers(List<String> excluded, double latitude, double longitude, int zoom, int width, int height) {
        LoggerUtils.debug("I'm initilizing external server layers!");
    	Map<String, Layer> externalLayers = new LinkedHashMap<String, Layer>();
        LayerJSONParser lp = new LayerJSONParser();
        String response = lp.parse(externalLayers, excluded, latitude, longitude, zoom, width, height);
        synchronized (layers) {
            layers.putAll(externalLayers);
        }
        return response;
    }

    public boolean layerExists(String layerName) {
        return layers.containsKey(layerName);
    }

    public static int getDealCategoryIcon(int categoryId, int type) {
        int icon = -1;
        try {
        	Category c = CategoriesManager.getInstance().getCategory(categoryId);
            if (c != null) {
            	if (type == LAYER_ICON_LARGE) {
            		icon = c.getLargeIcon();
            	} else {
            		icon = c.getIcon();
            	}
           }	
        } catch (Exception e) {
            LoggerUtils.error("LayerManager.getDealCategoryIcon() exception:", e);
        }
        if (icon == -1) {
        	if (type == LAYER_ICON_LARGE) {
        		icon = R.drawable.image_missing32;
        	} else {
        		icon = R.drawable.image_missing16;
        	}
        }
        return icon;
    }

    public static int getLayerIcon(String layerName, int type) {
    	int icon = -1;
    	
    	if (StringUtils.isNotEmpty(layerName)) {
        	Layer layer = layers.get(layerName);
        	if (layer != null) {
        		if (type == LAYER_ICON_LARGE) {
    				icon = layer.getLargeIconResource();
    			} else {
    				icon = layer.getSmallIconResource();
    			}	
        	}
    	}
    	
    	if (icon == -1) {
        	if (type == LAYER_ICON_LARGE) {
        		icon = R.drawable.image_missing32;
        	} else {
        		icon = R.drawable.image_missing16;
        	}
        }
    	
    	return icon;
    }
    
    public static int getLayerImage(String layerName) {
    	int imageId = 0;
    	if (StringUtils.isNotEmpty(layerName)) {
        	Layer layer = layers.get(layerName);
            if (layer != null) {
            	imageId = layer.getImage();
            } 
    	}    
    	return imageId;
    }
    
    public static String getLayerIconUri(String layerName, int type) {
    	String icon = null;
    	if (StringUtils.isNotEmpty(layerName)) {
        	Layer layer = layers.get(layerName);
        	if (type == LAYER_ICON_LARGE) {
    			icon = layer.getLargeIconPath();
    		} else {
    			icon = layer.getSmallIconPath();
    		}	
    	}
    	return icon;
    }
    
    public static BitmapDrawable getLayerIcon(String layerName, int type, DisplayMetrics displayMetrics, Handler handler) {
        BitmapDrawable layerDrawable = null;
        if (StringUtils.isNotEmpty(layerName)) {
        	try {
            	Layer layer = layers.get(layerName);
            	if (layer != null) {
            		if (type == LAYER_ICON_LARGE && (layer.getLargeIconPath() != null || layer.getLargeIconResource() != -1)) {
            	  		//layer has large icon
            	  		layerDrawable = IconCache.getInstance().getLayerImageResource(layer.getName(), "_large", layer.getLargeIconPath(),
                        layer.getLargeIconResource(), null, layer.getType(), displayMetrics, handler);
              		} else {
            	  		//SMALL icon default
            	  		layerDrawable = IconCache.getInstance().getLayerImageResource(layer.getName(), "_small", layer.getSmallIconPath(),
                        layer.getSmallIconResource(), null, layer.getType(), displayMetrics, handler);
              		}	
            	} 
        	} catch (Exception e) {
        		LoggerUtils.error("LayerManager.getLayerIcon() exception", e);
        	}
        }
        if (layerDrawable == null || layerDrawable.getBitmap() == null || layerDrawable.getBitmap().isRecycled()) {
        	if (type == LAYER_ICON_SMALL) {
        		layerDrawable = IconCache.getInstance().getImageDrawable(IconCache.ICON_MISSING16);
        	} else {
        		layerDrawable = IconCache.getInstance().getImageDrawable(IconCache.ICON_MISSING32);
        	}	
        }
        return layerDrawable;
    }

    public String getLayerDesc(String layerName) {
        if (layers.containsKey(layerName)) {
            Layer layer = layers.get(layerName);
            return layer.getDesc();
        }
        return null;
    }

    public String getLayerFormatted(String layerName) {
        if (layers.containsKey(layerName)) {
            Layer layer = layers.get(layerName);
            return layer.getFormatted();
        }
        return null;
    }

    public FileManager.ClearPolicy getClearPolicy(String layerName) {
        if (layers.containsKey(layerName)) {
            Layer layer = layers.get(layerName);
            return layer.getClearPolicy();
        }
        return null;
    }
    
    public Bundle loadLayersGroup() {
        Bundle extras = new Bundle();
        List<String> names = new ArrayList<String>();
        List<Boolean> enabled = new ArrayList<Boolean>();

        synchronized (layers) {
            for (Iterator<Map.Entry<String, Layer>> i = layers.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, Layer> entry = i.next();
                Layer layer = entry.getValue();

                if (layer.isManageable()) {
                    names.add(layer.getName());
                    enabled.add(layer.isEnabled());
                }
            }
        }

        int size = names.size();
        String[] codes = new String[size];
        String[] namesArray = new String[size];
        boolean[] enabledArray = new boolean[size];

        for (int i = 0; i < enabled.size(); i++) {
            enabledArray[i] = enabled.get(i);
            namesArray[i] = names.get(i);
            codes[i] = Integer.toString(i);

            //System.out.println(namesArray[i] + " " + enabledArray[i]);
        }

        extras.putStringArray("names", namesArray);
        extras.putBooleanArray("enabled", enabledArray);
        extras.putStringArray("codes", codes);

        return extras;
    }

    public void saveLayersAction(String[] names, String[] codes) {
        if (names != null && codes != null) {
            int maxj = codes.length;
            int j = 0;
            for (int i = 0; i < names.length; i++) {    //0 1 2 3 4
                //0   2   4
                if (j < maxj && Integer.parseInt(codes[j]) == i) {
                    //System.out.println(names[i] + " selected");
                    setLayerEnabled(names[i], true);
                    j++;
                } else {
                    if (j >= maxj || Integer.parseInt(codes[j]) > i) {
                        //System.out.println(names[i] + " unselected");
                        setLayerEnabled(names[i], false);
                    }
                }
            }
        }
    }
    
    private String[] getClassesOfPackage(String packageName) {
        ArrayList<String> classes = new ArrayList<String>();
        try {
        	DexFile df = new DexFile(ConfigurationManager.getInstance().getContext().getPackageCodePath());
        	for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ) {
                String className = iter.nextElement();
                if (className.contains(packageName)) {
                	classes.add(className);
                	//classes.add(className.substring(className.lastIndexOf(".") + 1, className.length()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return classes.toArray(new String[classes.size()]);
    }

    //DYNAMIC LAYERS
    
    private void initializeDynamicLayers() {
        String dl = ConfigurationManager.getInstance().getString(ConfigurationManager.DYNAMIC_LAYERS);
        if (StringUtils.isNotEmpty(dl)) {
            String[] dynamicLayersStr = StringUtils.split(dl, ConfigurationManager.LAYER_SEPARATOR);
            Map<String, Layer> dynLayers = new HashMap<String, Layer>();
            for (int i = 0; i < dynamicLayersStr.length; i++) {
                String[] tokens = StringUtils.split(dynamicLayersStr[i], ",");
                String layer = tokens[0];

                int res = getDynamicLayerIcon(tokens);

                int image = getDynamicLayerImage(layer);

                Layer newLayer = LayerFactory.getLayer(layer, false, true, false, null, null, res, null, -1, LAYER_DYNAMIC, null, StringUtils.capitalize(layer), FileManager.ClearPolicy.ONE_MONTH, image);
                newLayer.setKeywords(tokens);
                dynLayers.put(layer, newLayer);
            }
            if (!dynLayers.isEmpty()) {
                synchronized (layers) {
                    layers.putAll(dynLayers);
                    dynamicLayers.addAll(dynLayers.keySet());
                }
            }

        }
    }

    public List<String> getDynamicLayers() {
        return dynamicLayers;
    }

    public boolean addDynamicLayer(String keywords) {
        String dl = ConfigurationManager.getInstance().getString(ConfigurationManager.DYNAMIC_LAYERS);
        if (dl == null) {
            dl = "";
        }
        String[] tokens = StringUtils.split(keywords, ",");
        String layerName = tokens[0];

        boolean containsLayer = false;
        if (layers.containsKey(layerName)) {
            containsLayer = true;
        } else {
            if (StringUtils.isNotEmpty(dl)) {
                String[] dynamicLayersStr = StringUtils.split(dl, ConfigurationManager.LAYER_SEPARATOR);
                for (int i = 0; i < dynamicLayersStr.length; i++) {
                    String layer = StringUtils.split(dynamicLayersStr[i], ",")[0];
                    if (layer.equalsIgnoreCase(layerName)) {
                        containsLayer = true;
                        break;
                    }
                }
            }
        }

        if (!containsLayer) {
            if (StringUtils.isNotEmpty(dl)) {
                dl += ConfigurationManager.LAYER_SEPARATOR;
            }
            dl += keywords;
            ConfigurationManager.getInstance().putString(ConfigurationManager.DYNAMIC_LAYERS, dl);

            int res = getDynamicLayerIcon(tokens);
            
            int image = getDynamicLayerImage(layerName);

            Layer layer = LayerFactory.getLayer(layerName, false, true, false, null, null, res, null, -1, LAYER_DYNAMIC, null, StringUtils.capitalize(layerName), FileManager.ClearPolicy.ONE_MONTH, image);
            layer.setKeywords(tokens);

            synchronized (layers) {
                Map<String, Layer> copy = new LinkedHashMap<String, Layer>(layers);
                layers.clear();
                layers.put(layerName, layer);
                layers.putAll(copy);
                dynamicLayers.add(layerName);
            }
        }

        //System.out.println("Dynamic layers " + dl + " ----------------------------------------");
        return containsLayer;
    }

    public void removeDynamicLayer(String layerName) {
        String dl = ConfigurationManager.getInstance().getString(ConfigurationManager.DYNAMIC_LAYERS);
        if (StringUtils.isNotEmpty(dl)) {
            String newdl = "";
            String[] dynamicLayersStr = StringUtils.split(dl, ConfigurationManager.LAYER_SEPARATOR);
            for (int i = 0; i < dynamicLayersStr.length; i++) {
                String[] tokens = StringUtils.split(dynamicLayersStr[i], ",");
                if (!tokens[0].equalsIgnoreCase(layerName)) {
                    if (newdl.length() > 0) {
                        newdl += ConfigurationManager.LAYER_SEPARATOR;
                    }
                    newdl += dynamicLayersStr[i];
                }
            }
            dynamicLayers.remove(layerName);
            ConfigurationManager.getInstance().putString(ConfigurationManager.DYNAMIC_LAYERS, newdl);
        }
    }

    private static int getDynamicLayerIcon(String[] tokens) {

        Context c = ConfigurationManager.getInstance().getContext();
        if (c != null) {
            for (int i = 0; i < tokens.length; i++) {
                int res = c.getResources().getIdentifier(tokens[i].toLowerCase(java.util.Locale.US), "drawable", c.getPackageName());
                if (res > 0) {
                    return res;
                }
            }
        }

        return R.drawable.search;
    }
    
    private static int getDynamicLayerImage(String layer) {
    	Context c = ConfigurationManager.getInstance().getContext();
        if (c != null) {
        	String formattedName = StringUtils.replaceChars(layer.toLowerCase(java.util.Locale.US), ' ', '_');
            return c.getResources().getIdentifier(formattedName + "_img", "drawable", c.getPackageName());
        }
        return 0;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    
    private class LayerEnabledPredicate implements Predicate<String> {

        public boolean apply(String layer) {
            if (allLayers.containsKey(layer)) {
                return allLayers.get(layer).isEnabled();
            } else {
                return false;
            }
        }
    }

    private class LayerDisabledPredicate implements Predicate<String> {

        public boolean apply(String layer) {
            if (allLayers.containsKey(layer)) {
                return !allLayers.get(layer).isEnabled();
            } else {
                return false;
            }
        }
    }

    private class LayerEnabledInListPredicate implements Predicate<String> {

        private List<String> layers;

        public LayerEnabledInListPredicate(List<String> layers) {
            this.layers = layers;
        }

        public boolean apply(String layer) {
            if (allLayers.containsKey(layer) && layers.contains(layer)) {
                return allLayers.get(layer).isEnabled();
            } else {
                return false;
            }
        }
    }

    private class LayerDisabledInListPredicate implements Predicate<String> {

        private List<String> layers;

        public LayerDisabledInListPredicate(List<String> layers) {
            this.layers = layers;
        }

        public boolean apply(String layer) {
            if (allLayers.containsKey(layer) && layers.contains(layer)) {
                return !allLayers.get(layer).isEnabled();
            } else {
                return false;
            }
        }
    }

    private class LayerExternalNotInListPredicate implements Predicate<String> {

        private List<String> exLayers;

        public LayerExternalNotInListPredicate(List<String> layers) {
            this.exLayers = layers;
        }

        public boolean apply(String layerName) {
            Layer layer = layers.get(layerName);
            if (layer.getType() == LayerManager.LAYER_EXTERNAL && !exLayers.contains(layerName)) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    private class LayerReaderComparator implements Comparator<List<LayerReader>> {

		@Override
		public int compare(List<LayerReader> first, List<LayerReader> second) {
			return getPriority(first) - getPriority(second);
		}
		
		private int getPriority(List<LayerReader> readers) {
			int priority = readers.get(0).getPriority();
			if (readers.size() > 0) {
				for (LayerReader l : readers) {
					if (l.isPrimary()) {
						priority = l.getPriority();
						break;
					}
				}
			}
			return priority;
		}   	
    }
}
