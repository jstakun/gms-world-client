package com.jstakun.gms.android.landmarks;

import android.os.Handler;
import android.os.Message;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.FacebookUtils;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MessageCondition;
import com.jstakun.gms.android.utils.MessageStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class LayerLoader {
    
    public static final int LAYER_LOADED = 100;
    public static final int ALL_LAYERS_LOADED = 101;
    public static final int FB_TOKEN_EXPIRED = 102;
    
    private static final ConcurrentLayerLoader concurrentLayerLoader = new ConcurrentLayerLoader();
    private static final List<String> excludedExternal = new ArrayList<String>();
    private static final LayerLoader instance = new LayerLoader();
    
    private double latitude, longitude;
    private int zoom, width, height, counter;
    private boolean loadExternal = true, initialized = false;
    private Handler repaintHandler;
    private InitLayerLoadingTask initLayerLoadingTask;
    private int currentLayerIndex = -1;
    private long loadingStartTime;
    
    private LayerLoader() {
    }
    
    public static LayerLoader getInstance() {
    	return instance;
    }
    
    public void setRepaintHandler(Handler repaintHandler) {
        this.repaintHandler = repaintHandler;
    }
    
    private void sendLayerLoadedMessage(String layerKey) {
        if (repaintHandler != null) {
        	Message msg = new Message();
        	msg.what = LAYER_LOADED;
        	msg.obj = layerKey;
        	repaintHandler.sendMessage(msg);
        }
    }
    
    private void sendFinishedMessage() {
        if (repaintHandler != null) {
            repaintHandler.sendEmptyMessage(ALL_LAYERS_LOADED);
        }
    }
    
    public void loadLayers(double latitude, double longitude, int zoom, int width, int height, boolean loadExternal, String selectedLayer, boolean loadServerLayers) {
        
        this.height = height;
        this.latitude = latitude;
        this.longitude = longitude;
        this.width = width;
        this.zoom = zoom;
        this.loadExternal = loadExternal;
        initialized = false;
        
        loadingStartTime = System.currentTimeMillis();
        //System.out.println("LayerLoader.loadLayers()");

        if ((ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION) && loadExternal)
                || ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
        	MessageStack.getInstance().addMessage(Locale.getMessage(R.string.Layer_Loading), -1, MessageCondition.LAYER_LOADING, MessageStack.LOADING);
        }
        
        if (selectedLayer != null) {
            //currentLayerIndex = -1;
            LayerLoaderTask currentTask = new LayerLoaderTask();
            currentTask.execute(selectedLayer);
            concurrentLayerLoader.addTask(selectedLayer, currentTask);           
            initialized = true;
        } else {
        	initLayerLoadingTask = new InitLayerLoadingTask();
            initLayerLoadingTask.execute(loadServerLayers);
        }
    }
    
    private void notifyOnLoadingFinished(String layerKey) {
        //System.out.println("LayerLoader.notifyOnLoadingFinished() " + layerKey);

        boolean hasLayerLoaderTask = false;
        boolean hasLayerLoader = false;
        String key = null;
        
        if (currentLayerIndex != -1) {
            List<String> layers = LayerManager.getInstance().getLayers();
            while (currentLayerIndex < layers.size()) {
                key = layers.get(currentLayerIndex);
                Layer layer = LayerManager.getInstance().getLayer(key);
                List<LayerReader> layerReader = layer.getLayerReader();
                currentLayerIndex++;
                if (!layerReader.isEmpty()) {
                    hasLayerLoader = true;
                    break;
                } 
            }
            
            if (hasLayerLoader) {
                counter++;
                int size = LayerManager.getInstance().getLoadableLayersCount();
                //System.out.println("Loading layer " + key + " (" + counter + "/" + size + ")...");
                MessageStack.getInstance().addMessage(Locale.getMessage(R.string.Layer_Loading_counter, counter, size), -1, MessageCondition.LAYER_LOADING, MessageStack.LOADING);
                
                try {
                    LayerLoaderTask currentTask = new LayerLoaderTask();
                    currentTask.execute(key);
                    concurrentLayerLoader.addTask(key, currentTask);
                    hasLayerLoaderTask = true;
                } catch (Exception e) {
                    LoggerUtils.error("LayerLoader.notifyOnLoadingFinished exception:", e);
                }
            }
        }
        
        synchronized (concurrentLayerLoader) {
            concurrentLayerLoader.removeTask(layerKey);
            
            if (concurrentLayerLoader.isEmpty()) {
                if ((ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION) && loadExternal)
                        || (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION))) {
                    //System.out.println("Updating view...");
                	MessageStack.getInstance().addMessage(Locale.getQuantityMessage(R.plurals.LayerLoaded, ConfigurationManager.getInstance().getLandmarkManager().getAllLayersSize()), 3, -1, MessageStack.LAYER_LOADED);
                    long loadingTime = (System.currentTimeMillis() - loadingStartTime) / 1000;
                    ConfigurationManager.getInstance().putObject("LAYERS_LOADING_TIME_SEC", loadingTime);
                    sendFinishedMessage();
                    excludedExternal.clear();
                }
            } else if (!hasLayerLoaderTask) {
                //System.out.println("Finishing layer loading ...");
            	MessageStack.getInstance().addMessage(Locale.getMessage(R.string.Layer_Loading_processing), -1, MessageCondition.LAYER_LOADING, MessageStack.LOADING);
            }
        }
    }
    
    public boolean isLoading() {
        if (!concurrentLayerLoader.isEmpty() //|| !initialized
                || (currentLayerIndex != -1 && !concurrentLayerLoader.isCancelled()
                && currentLayerIndex < LayerManager.getInstance().getLayers().size())) {
            //LoggerUtils.debug("LayerLoader.isLoading() true");
            return true;
        } else {
        	//LoggerUtils.debug("LayerLoader.isLoading() false");
            return false;
        }
    }
    
    public void stopLoading() {
        //System.out.println("LayerLoader.stopLoading()");

        if (initLayerLoadingTask != null && initLayerLoadingTask.getStatus() == GMSAsyncTask.Status.RUNNING) {
            try {
                initLayerLoadingTask.cancel(true);
            } catch (Exception e) {
            }
        }
        
        if (concurrentLayerLoader != null) {
            try {
                concurrentLayerLoader.cancelAll();
            } catch (Exception e) {
            }
        }
        
        MessageStack.getInstance().removeConditionalMessage(true, MessageStack.LAYER_LOADED);
        
        excludedExternal.clear();
        currentLayerIndex = -1;
        counter = 0;
    }
    
    public boolean isInitialized() {
    	//LoggerUtils.debug("LayerLoader.isInitialized() " + initialized);
    	return initialized;
    }
    
    public void onAppClose() {
    	initialized = false;
    	currentLayerIndex = -1;
    	loadExternal = true;
    }
        
    private class LayerLoaderTask extends GMSAsyncTask<String, Void, String> {
        
        private LayerReader currentReader = null;
        
        public LayerLoaderTask() {
            super(1, LayerLoaderTask.class.getName());
        }
        
        @Override
        protected String doInBackground(String... args) {
            String key = args[0];
            if (!isCancelled() && key != null && !excludedExternal.contains(key)) {
                loadLayer(key, false, true);
            }
            return key;
        }
        
        @Override
        protected void onPostExecute(String key) {
            //System.out.println("LayerLoaderTask.onPostExecute() " + key);
            notifyOnLoadingFinished(key);
        }
        
        @Override
        protected void onCancelled(String key) {
            //System.out.println("LayerLoaderTask.onCancelled() " + key);
            concurrentLayerLoader.removeTask(key);
        }
        
        private void loadLayer(String key, boolean loadIfDisabled, boolean repaintIfNoError) {
            //System.out.println("LayerLoaderTask.loadLayer() " + key);
            Layer layer = LayerManager.getInstance().getLayer(key);
            if (layer != null && (loadIfDisabled || layer.isEnabled() || ConfigurationManager.getInstance().isOn(ConfigurationManager.LOAD_DISABLED_LAYERS))) {
                List<LayerReader> readers = layer.getLayerReader();
            	if (!readers.isEmpty()) {
                    if ((ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION) && loadExternal)
                            || ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
                        for (LayerReader reader : readers) {
                        	currentReader = reader;
                        	if (!concurrentLayerLoader.isCancelled() && !isCancelled()) {
                        		List<ExtendedLandmark> items = ConfigurationManager.getInstance().getLandmarkManager().getLandmarkStoreLayer(key);
                        		int initialSize = items.size();
                        		String errorMessage = reader.readRemoteLayer(items, latitude, longitude, zoom, width, height, key, this);
                            	if (errorMessage != null) {
                            		MessageStack.getInstance().addMessage(errorMessage, 10, -1, -1);
                        			if (repaintHandler != null && errorMessage.equals(FacebookUtils.FB_OAUTH_ERROR)) {
                        				repaintHandler.sendEmptyMessage(FB_TOKEN_EXPIRED);
                        			}
                        		} else if (repaintIfNoError && (items.size() > initialSize || StringUtils.equals(key, Commons.LOCAL_LAYER)) && layer.isEnabled()) {
                        			sendLayerLoadedMessage(key);
                        		}
                        	}
                        }
                    }
                }
            }
        }
        
        protected void closeReader() {
            if (currentReader != null) {
                currentReader.close();
            }
        }
    }
    
    private class InitLayerLoadingTask extends GMSAsyncTask<Boolean, Void, Void> {
        
        public InitLayerLoadingTask() {
            super(1, InitLayerLoadingTask.class.getName());
        }
        
        @Override
        protected Void doInBackground(Boolean... b) {

            //System.out.println("InitLayerLoadingTask.doInBackground()");

            Boolean loadServerLayers = false;
            if (b.length > 0) {
                loadServerLayers = b[0];
            }

            //load server layers after external layers
            if (loadServerLayers) {
                //System.out.println("Initializing server layer list...");
                if ((ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION) && loadExternal)
                        || ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
                    new ExternalLayersInitiatorTask().execute();
                }
            }
            
            currentLayerIndex = 0;
            counter = 0;
            initialized = true;
            int i = 0;
            final int maxConcurrentTasks = ConfigurationManager.getInstance().getInt(ConfigurationManager.LANDMARKS_CONCURRENT_COUNT, 3);
            
            while (i < maxConcurrentTasks && currentLayerIndex < LayerManager.getInstance().getLayers().size()) {
                try {
                    String key = LayerManager.getInstance().getLayers().get(currentLayerIndex);
                    Layer layer = LayerManager.getInstance().getLayer(key);
                    List<LayerReader> layerReader = layer.getLayerReader();
                    
                    currentLayerIndex++;
                    
                    if (!layerReader.isEmpty()) {
                        counter++;
                        if (!excludedExternal.contains(key)) {
                            i++;
                            //System.out.println("Loading layer " + key + " " + counter + "...");
                            int size = LayerManager.getInstance().getLoadableLayersCount();
                            MessageStack.getInstance().addMessage(Locale.getMessage(R.string.Layer_Loading_counter, counter, size), -1, MessageCondition.LAYER_LOADING, MessageStack.LOADING);
                            LayerLoaderTask currentTask = new LayerLoaderTask();
                            currentTask.execute(key);
                            concurrentLayerLoader.addTask(key, currentTask);
                        } else {
                        	//System.out.println("Skipping layer " + key);
                        }
                    }
                } catch (Exception e) {
                    LoggerUtils.error("InitLayerLoadingTask.doInBackground() exception: ", e);
                }
            }
            
            return null;
        }
    }
    
    private class ExternalLayersInitiatorTask extends GMSAsyncTask<String, Void, String> {
        
        public ExternalLayersInitiatorTask() {
            super(1, ExternalLayersInitiatorTask.class.getName());
        }
        
        @Override
        protected String doInBackground(String... arg0) {
            excludedExternal.clear();
            return LayerManager.getInstance().initializeExternalLayers(excludedExternal, latitude, longitude, zoom, width, height);
        }
        
        @Override
        protected void onPostExecute(String errorMessage) {
            if (errorMessage != null) {
            	MessageStack.getInstance().addMessage(errorMessage, 10, -1, -1);
            }
        }
    }
    
    private static class ConcurrentLayerLoader {
        
        private Map<String, LayerLoaderTask> tasksInProgress = new HashMap<String, LayerLoaderTask>();
        private boolean isCancelled = false;
        
        protected void addTask(String layer, LayerLoaderTask task) {
            //System.out.println("ConcurrentLayerLoader.addTask() " + layer);
            tasksInProgress.put(layer, task);
            isCancelled = false;
        }
        
        protected void removeTask(String layer) {
            //System.out.println("ConcurrentLayerLoader.removeTask() " + layer);
            tasksInProgress.remove(layer);
        }
        
        protected boolean isEmpty() {
            return tasksInProgress.isEmpty();
        }
        
        //protected boolean isFull() {
        //    return (tasksInProgress.size() == MAX_CONCURRENT_TASKS);
        //}
        
        protected void cancelAll() {
            //System.out.println("ConcurrentLayerLoader.cancelAll()");
            isCancelled = true;
            for (Iterator<Map.Entry<String, LayerLoaderTask>> i = tasksInProgress.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, LayerLoaderTask> taskEntry = i.next();
                LayerLoaderTask task = taskEntry.getValue();
                task.closeReader();
                task.cancel(true);
                i.remove();
            }
        }
        
        protected boolean isCancelled() {
            return isCancelled;
        }
    }
}
