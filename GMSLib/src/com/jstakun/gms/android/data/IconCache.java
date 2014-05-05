/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.data;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;

import com.jstakun.gms.android.config.BCTools;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class IconCache {

    public static final String ICON_MISSING32 = "icon-missing32";
    public static final String ICON_MISSING16 = "icon-missing16";
    private static final String LOADING = "loading";
    //public static final String LOADING_ICON = "loading-icon";
    private static final String COMPASS = "compass";
    private static final String DOWNLOAD = "download";
    //private static final String DOWNLOAD48 = "download48";
    public static final String CURSOR = "cursor";
    public static final String MAGNIFIER = "magnifier";
    //public static final String BULLET = "bullet";
    private static final String GRID = "grid";
    public static final String IMAGE_LOADING_TILE = "image-loading-tile";
    public static final String IMAGE_LOADING_MAP = "image-loading-map";
    public static final String IMAGE_MISSING = "image-missing";
    //private BitmapDrawable[] compass = new BitmapDrawable[16];
    private Map<String, Bitmap> images = new HashMap<String, Bitmap>();
    private Map<String, GMSAsyncTask<?,?,?>> loadingTasks = new HashMap<String, GMSAsyncTask<?,?,?>>();
    private static IconCache instance;
    private static Paint paint;
    //private static BitmapFactory.Options bitmapOptions = new BitmapFactory.Options(); 

    //static {
    //    bitmapOptions.inScaled = false;
    //}
    /**
     * Private Constructor! Creates a new instance of IconCache
     */
    private IconCache() {
        try {
            Context ctx = ConfigurationManager.getInstance().getContext();

            if (!images.containsKey(CURSOR)) {
                images.put(CURSOR, BitmapFactory.decodeResource(ctx.getResources(), R.drawable.pointer16));
            }
            if (!images.containsKey(MAGNIFIER)) {
                images.put(MAGNIFIER, BitmapFactory.decodeResource(ctx.getResources(), R.drawable.magnifier));
            }
            if (!images.containsKey(ICON_MISSING32)) {
                images.put(ICON_MISSING32, BitmapFactory.decodeResource(ctx.getResources(), R.drawable.image_missing32));
            }
            if (!images.containsKey(ICON_MISSING16)) {
                images.put(ICON_MISSING16, BitmapFactory.decodeResource(ctx.getResources(), R.drawable.image_missing16));
            }
            //if (!images.containsKey(BULLET)) {
            //    images.put(BULLET, BitmapFactory.decodeResource(ctx.getResources(), R.drawable.bullet));
            //}
            if (!images.containsKey(LOADING)) {
                images.put(LOADING, BitmapFactory.decodeResource(ctx.getResources(), R.drawable.loading));
            }
            if (!images.containsKey(COMPASS)) {
                images.put(COMPASS, BitmapFactory.decodeResource(ctx.getResources(), R.drawable.compass_new));
            }
            if (!images.containsKey(DOWNLOAD)) {
                images.put(DOWNLOAD, BitmapFactory.decodeResource(ctx.getResources(), R.drawable.download));
            }
            //if (!images.containsKey(LOADING_ICON)) {
            //    images.put(LOADING_ICON, BitmapFactory.decodeResource(ctx.getResources(), R.drawable.loading_icon));
            //}
            if (!images.containsKey(GRID)) {
                images.put(GRID, BitmapFactory.decodeResource(ctx.getResources(), R.drawable.grid));
            }

            paint = new Paint();
            paint.setAntiAlias(true);

            if (!images.containsKey(IMAGE_LOADING_TILE)) {
                Bitmap loading = Bitmap.createBitmap(ConfigurationManager.TILE_SIZE, ConfigurationManager.TILE_SIZE, ConfigurationManager.getInstance().getBitmapConfig());
                Canvas c = new Canvas(loading);
                c.drawColor(Color.WHITE);
                paint.setColor(Color.WHITE);
                c.drawRect(new Rect(0, 0, ConfigurationManager.TILE_SIZE, ConfigurationManager.TILE_SIZE), paint);
                c.drawBitmap(getImageResource(IconCache.LOADING), (ConfigurationManager.TILE_SIZE - 32) / 2, (ConfigurationManager.TILE_SIZE - 32) / 2, paint);
                images.put(IMAGE_LOADING_TILE, loading);
            }

            if (!images.containsKey(IMAGE_MISSING)) {
                Bitmap missing = Bitmap.createBitmap(ConfigurationManager.TILE_SIZE, ConfigurationManager.TILE_SIZE, ConfigurationManager.getInstance().getBitmapConfig());
                Canvas cm = new Canvas(missing);
                cm.drawColor(Color.WHITE);
                Paint paintm = new Paint();
                paintm.setColor(Color.WHITE);
                cm.drawBitmap(getImageResource(IconCache.ICON_MISSING32), (ConfigurationManager.TILE_SIZE - 32) / 2, (ConfigurationManager.TILE_SIZE - 32) / 2, paintm);
                images.put(IMAGE_MISSING, missing);
            }

        } catch (Exception ex) {
            LoggerUtils.error("IconCache.IconCache exception", ex);
        }
    }

    /**
     * Get singleton instance
     */
    public static IconCache getInstance() {
        if (instance == null) {
            instance = new IconCache();
        }
        return instance;
    }

    private Bitmap getImageResource(String resourceName) {
        Bitmap img;

        if (resourceName != null && isImageLoaded(resourceName)) {
            img = images.get(resourceName);
        } else {
            img = images.get(ICON_MISSING32);
        }

        return img; 
    }
    
    public final BitmapDrawable getImageDrawable(String resourceName) {
    	return getBitmapDrawable(getImageResource(resourceName));
    }

    private boolean isImageLoaded(String resourceName) {
        /*if (images.containsKey(resourceName)) {
        	return (!images.get(resourceName).isRecycled());
        } else {
        	return false;
        }*/
    	return (images.containsKey(resourceName));
    }

    public BitmapDrawable getLayerImageResource(String layerName, String suffix, String uri, int resourceId, String resourceIdStr, int type, DisplayMetrics displayMetrics, Handler handler) {
        Bitmap img = null;
        boolean serverLoading = false;
        String resourceName = layerName + suffix;
        if (isImageLoaded(resourceName)) {
            img = images.get(resourceName);
        } else {
            if (type == LayerManager.LAYER_LOCAL || type == LayerManager.LAYER_DYNAMIC) {
                try {
                    int res = resourceId;
                    Context c = ConfigurationManager.getInstance().getContext();
                    if (c != null) {
                        Resources r = c.getResources();
                        if (res == -1 && resourceIdStr != null) {
                            res = r.getIdentifier(resourceIdStr, "drawable", c.getPackageName());
                        }
                        if (res > 0) {
                            img = BitmapFactory.decodeResource(r, res);
                        }
                    }
                } catch (Exception ex) {
                    LoggerUtils.error("IconCache.getLayerImageResource() exception 0", ex);
                }
            } else if (type == LayerManager.LAYER_EXTERNAL) {
                if (!loadingTasks.containsKey(resourceName)) {
                    //check if image exists in file cache
                    try {
                        URL imageURL = new URL(uri);
                        String[] pathTokens = imageURL.getFile().split("/");
                        img = PersistenceManagerFactory.getFileManager().readImageFile(FileManager.getIconsFolderPath(), pathTokens[pathTokens.length - 1], displayMetrics);
                    } catch (Exception ex) {
                        LoggerUtils.error("IconCache.getLayerImageResource() exception 1", ex);
                    }
                    if (ConfigurationManager.getInstance().isNetworkModeAccepted()) {
                    	if (img == null) {
                    		LoadExternalImageTask loadingTask = new LoadExternalImageTask(displayMetrics, false, handler);
                    		loadingTask.execute(layerName, suffix, uri);
                    		loadingTasks.put(resourceName, loadingTask);
                    	}
                    } else {
                    	LoggerUtils.debug("Skipping image loading " + uri + " due to lack of wi-fi...");
                    }
                } else if (loadingTasks.containsKey(resourceName)) {
                    LoadExternalImageTask loadingTask = (LoadExternalImageTask) loadingTasks.get(resourceName);
                    loadingTask.setHandler(handler);
                }

                if (img == null) {
                    serverLoading = true;
                }
            } else if (type == LayerManager.LAYER_FILESYSTEM) {
                img = PersistenceManagerFactory.getFileManager().readImageFile(FileManager.getIconsFolderPath(), uri, displayMetrics);
            }

            if (img != null) {
                images.put(resourceName, img);
            }

            if (serverLoading) {
                img = images.get(DOWNLOAD);
            } else if (img == null || img.isRecycled()) {
                img = images.get(ICON_MISSING16);
            }
        }
        return getBitmapDrawable(img);
    }

    public Bitmap getThumbnailResource(String urlString, String layer, DisplayMetrics displayMetrics, Handler handler) {
        String hash = null;

        if (StringUtils.isNotEmpty(urlString)) {
        	try {
        		URL url = new URL(urlString);
        		hash = BCTools.getMessageDigest(url.toString()) + "_" + layer;
        	} catch (Exception ex) {
        		LoggerUtils.error("IconCache.getThumbnailResource() exception for url " + urlString + "!", ex);
        	}
        }

        Bitmap img = null;

        if (hash != null) {
        	//System.out.println("Searching for " + hash + "...");
            if (isImageLoaded(hash)) {
            	//System.out.println(hash + " already in cache...");
                img = images.get(hash);
            } else if (!loadingTasks.containsKey(hash)) {
                //check if image exists in file cache
                try {
                	//System.out.println("Trying to find " + hash + " cache...");
                    img = PersistenceManagerFactory.getFileManager().readImageFileFromCache(hash, displayMetrics);
                } catch (Exception ex) {
                    LoggerUtils.error("IconCache.getThumbnailResource() exception reading image file from cache", ex);
                }

                if (img != null) {
                	//System.out.println("Found " + hash + " in cache");
                    images.put(hash, img);
                }
                if (img == null) {
                	//System.out.println("Loading " + hash + " from remote server...");
                	if (ConfigurationManager.getInstance().isNetworkModeAccepted()) {                        
                		try {
                			LoadExternalImageTask loadingTask = new LoadExternalImageTask(displayMetrics, true, handler);
                			loadingTask.execute(hash, "", urlString);
                			loadingTasks.put(hash, loadingTask);
                		} catch (Exception ex) {
                			LoggerUtils.error("IconCache.getThunbnailResource() exception running LoadExternalImageTask", ex);
                		}
                	} else {
                		LoggerUtils.debug("Skipping image loading " + urlString + " due to lack of wi-fi...");
                	}
                }
            } else if (loadingTasks.containsKey(hash)) {
            	//System.out.println(hash + " search in progress...");
            	LoadExternalImageTask loadingTask = (LoadExternalImageTask) loadingTasks.get(hash);
                loadingTask.setHandler(handler);
            }
        }
        return img;
    }

    public void clearImageCache() {
        images.clear();
    }

    protected void setResource(String name, Bitmap resource) {
        images.put(name, resource);
    }

    public Drawable getLayerBitmap(BitmapDrawable bd, String layerName, int color, boolean frame, DisplayMetrics displayMetrics) {
        if (frame) {
            String resourceName = layerName + "_selected_" + Integer.toString(color);
            if (isImageLoaded(resourceName)) {
                return getImageDrawable(resourceName);
            } else {
                Context ctx = ConfigurationManager.getInstance().getContext();
                if (ctx != null) {
                    return createLayerBitmap(ctx, bd.getBitmap(), color, resourceName);
                } else {
                    return null;
                }
            }
        } else {
            return bd;
        }
    }

    public Drawable getLayerBitmap(int res, String layerName, int color, boolean frame, DisplayMetrics displayMetrics) {
        Context ctx = ConfigurationManager.getInstance().getContext();
        if (ctx != null) {
            if (frame) {
                String resourceName = layerName + "_selected_" + Integer.toString(color);
                if (isImageLoaded(resourceName)) {
                    return getImageDrawable(resourceName);
                } else {
                    Bitmap b = BitmapFactory.decodeResource(ctx.getResources(), res);
                    return createLayerBitmap(ctx, b, color, resourceName);
                }
            } else {
                return ctx.getResources().getDrawable(res); //getBitmapDrawable(BitmapFactory.decodeResource(ctx.getResources(), res));
            }
        } else {
            return null;
        }
    }

    private Drawable createLayerBitmap(Context ctx, Bitmap b, int color, String resourceName) {
        Bitmap bottom = images.get(GRID);
        final int bottomSpace = (bottom.getHeight() / 2) + 5;
        int w = 4 * b.getWidth() / 3;
        int h = 4 * b.getHeight() / 3 + bottomSpace;

        //System.out.println(b.getWidth() + " x " + b.getHeight() + ", " + w + " x " + h + " ---------------------------------- " + resourceName);

        Bitmap bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        //draw bottom bitmap       
        c.drawBitmap(bottom, (w - bottom.getWidth()) / 2, h - bottom.getHeight(), paint);

        RectF rect = new RectF();
        rect.left = 0;
        rect.right = w;
        rect.top = 0;
        rect.bottom = h - bottomSpace;

        //fill rect
        int ovalx = b.getWidth() / 3;
        int ovaly = b.getHeight() / 3;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawRoundRect(rect, ovalx, ovaly, paint);

        //fill triange
        Path triangle = new Path();
        float ovalx_half = (ovalx * 0.5f);
        triangle.moveTo(w / 2 - ovalx_half, h - bottomSpace);
        triangle.lineTo(w / 2, h - (bottomSpace / 2));
        triangle.lineTo(w / 2 + ovalx_half, h - bottomSpace);
        triangle.lineTo(w / 2 - ovalx_half, h - bottomSpace);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawPath(triangle, paint);

        paint.setStyle(Paint.Style.STROKE);
        //paint.setStrokeWidth(2);
        if (ctx != null) {
            paint.setColor(ctx.getResources().getColor(R.color.trans_dim_gray));
        }

        //draw rect & triangle bounds
        Path bounds = new Path();
        bounds.moveTo(w - ovalx, h - bottomSpace);
        bounds.lineTo(w / 2 + ovalx_half, h - bottomSpace);
        bounds.lineTo(w / 2, h - (bottomSpace / 2));
        bounds.lineTo(w / 2 - ovalx_half, h - bottomSpace);
        bounds.lineTo(ovalx, h - bottomSpace);

        c.drawLine(ovalx, 0, w - ovalx, 0, paint);
        c.drawLine(w, ovaly, w, h - bottomSpace - ovaly, paint);
        c.drawLine(0, ovaly, 0, h - bottomSpace - ovaly, paint);

        int two_ovalx = 2 * ovalx;
        int two_ovaly = 2 * ovaly;
        c.drawArc(new RectF(0, 0, two_ovalx, two_ovaly), 180, 90, false, paint);
        c.drawArc(new RectF(w - two_ovalx, 0, w, two_ovaly), 270, 90, false, paint);
        c.drawArc(new RectF(0, h - bottomSpace - two_ovaly, two_ovalx, h - bottomSpace), 90, 90, false, paint);
        c.drawArc(new RectF(w - two_ovalx, h - bottomSpace - two_ovaly, w, h - bottomSpace), 0, 90, false, paint);

        c.drawPath(bounds, paint);

        if (!b.isRecycled()) {
        	//draw layer bitmap
        	Rect src = new Rect(0, 0, b.getWidth(), b.getHeight());
        	Rect dest = new Rect(src);
        	dest.offset(b.getWidth() / 6, b.getHeight() / 6);
        	c.drawBitmap(b, src, dest, paint);
        }
        
        //if b == DOWNLOAD don't cache
        if (!b.equals(images.get(DOWNLOAD))) {
            setResource(resourceName, bmp);
        }

        return getBitmapDrawable(bmp);
    }

    public void clearAll() {
    }

    private class LoadExternalImageTask extends GMSAsyncTask<String, Void, Void> {

        private DisplayMetrics displayMetrics;
        private boolean isImage = false;
        private Handler handler;

        public LoadExternalImageTask(DisplayMetrics displayMetrics, boolean isImage, Handler handler) {
            super(3);
            this.displayMetrics = displayMetrics;
            this.isImage = isImage;
            this.handler = handler;
        }

        public void setHandler(Handler handler) {
            this.handler = handler;
        }

        @Override
        protected Void doInBackground(String... args) {
            String layer = args[0];
            String resourceName = layer + args[1];
            String url = args[2];

            InputStream input = null;
            HttpUtils utils = null;
            Bitmap img = null;

            try {
                utils = new HttpUtils();
                byte[] file = utils.loadHttpFile(url, false, "image");
                if (file != null) {
                    input = new ByteArrayInputStream(file);
                    Bitmap b = BitmapFactory.decodeStream(input);
                    //save image to file cache
                    if (!isImage) {
                        URL imageURL = new URL(url);
                        String[] pathTokens = imageURL.getFile().split("/");
                        PersistenceManagerFactory.getFileManager().saveIconFile(b, pathTokens[pathTokens.length - 1]);
                    } else {
                        PersistenceManagerFactory.getFileManager().saveImageFileToCache(b, layer);
                    }
                    //
                    if (b != null && !b.isRecycled()) {
                        if (displayMetrics.density != 1f) {
                            int newWidth = (int) (b.getWidth() * displayMetrics.density);
                            int newHeight = (int) (b.getHeight() * displayMetrics.density);
                            img = Bitmap.createScaledBitmap(b, newWidth, newHeight, true);
                            b.recycle();
                        } else {
                            img = b;
                        }
                    }
                }
            } catch (Throwable ex) {
                LoggerUtils.error("IconCache.getLayerImageResource exception 1", ex);
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ex) {
                }
                try {
                    if (utils != null) {
                        utils.close();
                    }
                } catch (IOException ex) {
                }
                if (img != null) {
                    images.put(resourceName, img);
                    images.remove(layer + "_selected_true");
                    images.remove(layer + "_selected_false");
                    if (handler != null) {
                        //System.out.println("Sending message to handler " + url + " -----------------------------------");
                        Message msg = handler.obtainMessage();
                        msg.getData().putString("url", url);
                        handler.sendMessage(msg);
                    }

                }
                loadingTasks.remove(resourceName);
            }
            return null;
        }
    }
    
    private static BitmapDrawable getBitmapDrawable(Bitmap bitmap) {
    	try {
    		//API version >= 4
    		Context ctx = ConfigurationManager.getInstance().getContext();
    		return BitmapDrawableHelperInternal.getBitmapDrawable(bitmap, ctx.getResources());
    	} catch (Throwable e) {
    		//API version 3
    		return new BitmapDrawable(bitmap);
    	}
    }
    
    private static class BitmapDrawableHelperInternal { 
        private static BitmapDrawable getBitmapDrawable(Bitmap bitmap, Resources res) {
            return new BitmapDrawable(res, bitmap);
        }
    }
}
