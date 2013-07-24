/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import android.os.Handler;

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

/**
 *
 * @author jstakun
 */
public class LayerLoader {
    
    public static final int LAYER_LOADED = 100;
    public static final int ALL_LAYERS_LOADED = 101;
    public static final int FB_TOKEN_EXPIRED = 102;
    private double latitude, longitude;
    private int zoom, width, height, counter;
    private boolean loadExternal = true, initialized = false;
    private LandmarkManager landmarkManager;
    private MessageStack messageStack;
    private Handler repaintHandler;
    private final ConcurrentLayerLoader concurrentLayerLoader;
    private InitLayerLoadingTask initLayerLoadingTask;
    private static int MAX_CONCURRENT_TASKS;
    private List<String> excludedExternal;
    private int currentLayerIndex = 0;
    private long loadingStartTime;
    private Map<String, Integer> currentLayerReaderIndex;
    
    public LayerLoader(LandmarkManager landmarkManager, MessageStack messageStack) {
        //System.out.println("Creating layer loader object..");
        MAX_CONCURRENT_TASKS = ConfigurationManager.getInstance().getInt(ConfigurationManager.LANDMARKS_CONCURRENT_COUNT, 3);
        this.messageStack = messageStack;
        this.landmarkManager = landmarkManager;
        concurrentLayerLoader = new ConcurrentLayerLoader();
        excludedExternal = new ArrayList<String>();
        currentLayerReaderIndex = new HashMap<String, Integer>();
    }
    
    public void setRepaintHandler(Handler repaintHandler) {
        this.repaintHandler = repaintHandler;
    }
    
    private void sendRepaintMessage(String layerKey) {
        if (repaintHandler != null) {
            repaintHandler.sendEmptyMessage(LAYER_LOADED);
        }
    }
    
    private void sendFinishedMessage() {
        if (repaintHandler != null) {
            //System.out.println("Sending Finished Message... -----------------------------------------");
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
        //System.out.println("Layer loader loadLayers");

        if ((ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION) && loadExternal)
                || ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
            messageStack.addMessage(Locale.getMessage(R.string.Layer_Loading), -1, MessageCondition.LAYER_LOADING, MessageStack.LOADING);
        }
        
        if (selectedLayer != null) {
            currentLayerIndex = -1;
            Layer layer = landmarkManager.getLayerManager().getLayer(selectedLayer);
            for (int i = 0; i < layer.getLayerReader().size(); i++) {
                LayerLoaderTask currentTask = new LayerLoaderTask();
                currentTask.execute(selectedLayer, Integer.toString(i));
                //if (AsyncTaskExecutor.execute(currentTask, null, selectedLayer, Integer.toString(i))) {
                concurrentLayerLoader.addTask(selectedLayer + "_" + i, currentTask);
                //}
            }
            initialized = true;
        } else {
            initLayerLoadingTask = new InitLayerLoadingTask();
            initLayerLoadingTask.execute(loadServerLayers);
            //AsyncTaskExecutor.executeTask(initLayerLoadingTask, null, loadServerLayers);
        }
    }
    
    private void notifyOnLoadingFinished(String layerKey) {
        //System.out.println("Layer notifyOnLoadingFinished " + layerKey + " ----------------------");

        boolean hasLayerLoaderTask = false;
        boolean hasLayerLoader = false;
        String key = null;
        
        if (currentLayerIndex != -1) {
            List<String> layers = landmarkManager.getLayerManager().getLayers();
            while (currentLayerIndex < layers.size()) {
                key = layers.get(currentLayerIndex);
                Layer layer = landmarkManager.getLayerManager().getLayer(key);
                List<LayerReader> layerReader = layer.getLayerReader();
                
                nextReader(key, layerReader);
                
                if (layer != null && !layerReader.isEmpty() && layerReader.size() > currentLayerReaderIndex.get(key)) {
                    hasLayerLoader = true;
                    break;
                } //else {
                //System.out.println("Skipping layer " + key);
                //counter++;
                //}
            }
            
            if (hasLayerLoader) {
                counter++;
                int size = landmarkManager.getLayerManager().getLoadableLayersCount();
                //System.out.println("Loading layer " + key + " (" + counter + "/" + size + ")...");
                messageStack.addMessage(Locale.getMessage(R.string.Layer_Loading_counter, counter, size), -1, MessageCondition.LAYER_LOADING, MessageStack.LOADING);
                
                try {
                    LayerLoaderTask currentTask = new LayerLoaderTask();
                    int index = currentLayerReaderIndex.get(key);
                    currentTask.execute(key, Integer.toString(index));
                    //if (AsyncTaskExecutor.execute(currentTask, null, key, Integer.toString(index))) {
                    concurrentLayerLoader.addTask(key + "_" + index, currentTask);
                    //}
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
                    messageStack.addMessage(Locale.getMessage(R.string.Layer_Loaded, landmarkManager.getAllLayersSize()), 3, -1, MessageStack.LAYER_LOADED);
                    long loadingTime = (System.currentTimeMillis() - loadingStartTime) / 1000;
                    ConfigurationManager.getInstance().putObject("LAYERS_LOADING_TIME_SEC", loadingTime);
                    sendFinishedMessage();
                    excludedExternal.clear();
                }
            } else if (!hasLayerLoaderTask) {
                //System.out.println("Finishing layer loading ...");
                messageStack.addMessage(Locale.getMessage(R.string.Layer_Loading_processing), -1, MessageCondition.LAYER_LOADING, MessageStack.LOADING);
            }
        }
    }
    
    public boolean isLoading() {
        if (!concurrentLayerLoader.isEmpty() || !initialized
                || (currentLayerIndex != -1 && !concurrentLayerLoader.isCancelled()
                && currentLayerIndex < landmarkManager.getLayerManager().getLayers().size())) {
            //System.out.println("Layer loader isLoading true");
            return true;
        } else {
            //System.out.println("Layer loader isLoading false");
            return false;
        }
    }
    
    public void stopLoading() {
        //System.out.println("Layer loader stopLoading");

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
        
        messageStack.removeConditionalMessage(true, MessageStack.LAYER_LOADED);
        
        excludedExternal.clear();
        currentLayerIndex = 0;
        counter = 0;
        currentLayerReaderIndex.clear();
    }
    
    private void nextReader(String key, List<LayerReader> layerReader) {
        if (layerReader.isEmpty()) {
            currentLayerIndex++;
        } else if (currentLayerReaderIndex.containsKey(key) && layerReader.size() <= currentLayerReaderIndex.get(key)) {
            currentLayerIndex++;
        } else if (!currentLayerReaderIndex.containsKey(key) && layerReader.size() == 1) {
            currentLayerReaderIndex.put(key, 0);
            currentLayerIndex++;
        } else if (!currentLayerReaderIndex.containsKey(key) && layerReader.size() > 1) {
            currentLayerReaderIndex.put(key, 0);
        } else if (currentLayerReaderIndex.containsKey(key)) {
            currentLayerReaderIndex.put(key, currentLayerReaderIndex.get(key) + 1);
        }

        //System.out.println(currentLayerIndex + " " + currentLayerReaderIndex.get(key) + " " + layerReader.size());
    }
    
    private class LayerLoaderTask extends GMSAsyncTask<String, Void, String> {
        
        private LayerReader currentReader = null;
        private int index;
        
        public LayerLoaderTask() {
            super(1);
        }
        
        @Override
        protected String doInBackground(String... args) {
            String key = args[0];
            index = Integer.valueOf(args[1]).intValue();
            if (!isCancelled() && key != null && !excludedExternal.contains(key)) {
                loadLayer(key, false, true);
            }
            return key + "_" + index;
        }
        
        @Override
        protected void onPostExecute(String key) {
            //System.out.println("On post execute " + key);
            //concurrentLayerLoader.removeTask(key);
            notifyOnLoadingFinished(key);
        }
        
        @Override
        protected void onCancelled(String key) {
            //System.out.println("On cancelled " + key);
            concurrentLayerLoader.removeTask(key);
        }
        
        private void loadLayer(String key, boolean loadIfDisabled, boolean repaintIfNoError) {
            //System.out.println("LayerLoaderTask.loadLayer " + key);
            Layer layer = landmarkManager.getLayerManager().getLayer(key);
            if (layer != null && (loadIfDisabled || layer.isEnabled() || ConfigurationManager.getInstance().isOn(ConfigurationManager.LOAD_DISABLED_LAYERS))) {
                if (!layer.getLayerReader().isEmpty() && layer.getLayerReader().size() > index) {
                    if ((ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION) && loadExternal)
                            || ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
                        currentReader = layer.getLayerReader().get(index);
                        String errorMessage = currentReader.readRemoteLayer(landmarkManager.getLandmarkStoreLayer(key), latitude, longitude, zoom, width, height, key, this);
                        if (!isCancelled()) {
                            if (errorMessage != null) {
                                messageStack.addMessage(errorMessage, 10, -1, -1);
                                if (repaintHandler != null && errorMessage.equals(FacebookUtils.FB_OAUTH_ERROR)) {
                                    //Message msg = repaintHandler.obtainMessage();
                                    //msg.getData().putString("status", Locale.getMessage(R.string.Social_fb_token_expired));
                                    repaintHandler.sendEmptyMessage(FB_TOKEN_EXPIRED);
                                }
                            } else if (repaintIfNoError && layer.isEnabled()) {
                                sendRepaintMessage(key);
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
            super(1);
        }
        
        @Override
        protected Void doInBackground(Boolean... b) {

            //System.out.println("InitLayerLoadingTask.doInBackground...");

            Boolean loadServerLayers = false;
            if (b.length > 0) {
                loadServerLayers = b[0];
            }

            //load server layers after external layers
            if (loadServerLayers) {
                //System.out.println("Initializing server layer list...");
                if ((ConfigurationManager.getInstance().isOn(ConfigurationManager.FOLLOW_MY_POSITION) && loadExternal)
                        || ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
                    //AsyncTaskExecutor.execute(new ExternalLayersInitiatorTask(), null);
                    new ExternalLayersInitiatorTask().execute();
                }
            }
            
            currentLayerIndex = 0;
            currentLayerReaderIndex.clear();
            counter = 0;
            initialized = true;
            int i = 0;
            LayerManager layerManager = landmarkManager.getLayerManager();
            
            while (i < MAX_CONCURRENT_TASKS && currentLayerIndex < layerManager.getLayers().size()) {
                try {
                    String key = layerManager.getLayers().get(currentLayerIndex);
                    Layer layer = layerManager.getLayer(key);
                    List<LayerReader> layerReader = layer.getLayerReader();
                    
                    nextReader(key, layerReader);
                    
                    if (!layerReader.isEmpty() && layerReader.size() > currentLayerReaderIndex.get(key)) {
                        counter++;
                        if (!excludedExternal.contains(key)) {
                            i++;
                            //System.out.println("Loading layer " + key + " " + counter + "...");
                            int size = layerManager.getLoadableLayersCount();
                            messageStack.addMessage(Locale.getMessage(R.string.Layer_Loading_counter, counter, size), -1, MessageCondition.LAYER_LOADING, MessageStack.LOADING);
                            LayerLoaderTask currentTask = new LayerLoaderTask();
                            int index = currentLayerReaderIndex.get(key);
                            currentTask.execute(key, Integer.toString(index));
                            //if (AsyncTaskExecutor.execute(currentTask, null, key, Integer.toString(index))) {
                            concurrentLayerLoader.addTask(key + "_" + index, currentTask);
                            //}
                        }
                        // else {
                        //System.out.println("Skipping layer " + key);
                        //}
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
            super(1);
        }
        
        @Override
        protected String doInBackground(String... arg0) {
            excludedExternal.clear();
            return landmarkManager.getLayerManager().initializeExternalLayers(excludedExternal, latitude, longitude, zoom, width, height);
        }
        
        @Override
        protected void onPostExecute(String errorMessage) {
            if (errorMessage != null) {
                messageStack.addMessage(errorMessage, 10, -1, -1);
            }
        }
    }
    
    private class ConcurrentLayerLoader {
        
        private Map<String, LayerLoaderTask> tasksInProgress = new HashMap<String, LayerLoaderTask>(MAX_CONCURRENT_TASKS);
        private boolean isCancelled = false;
        
        protected void addTask(String layer, LayerLoaderTask task) {
            //System.out.println("Adding task " + layer);
            tasksInProgress.put(layer, task);
            isCancelled = false;
        }
        
        protected void removeTask(String layer) {
            //System.out.println("Removed task " + layer);
            tasksInProgress.remove(layer);
        }
        
        protected boolean isEmpty() {
            return tasksInProgress.isEmpty();
        }
        
        //protected boolean isFull() {
        //    return (tasksInProgress.size() == MAX_CONCURRENT_TASKS);
        //}
        
        protected void cancelAll() {
            //System.out.println("Cancel All");
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
