/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.maps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.PersistenceManager;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MercatorUtils;

/**
 *
 * @author jstakun
 */
// 0 1 2
// 3 4 5
// 6 7 8
//  0  1  2  3  4
//  5  6  7  8  9
// 10 11 12 13 14
// 15 16 17 18 19
// 20 21 22 23 24
public class TilesCache {

    public static final int TILES_CACHE_SMALL = 9;
    public static final int TILES_CACHE_LARGE = 25;
    public static final int[][] TILES_GRID_9 = {{0, 0, 4}, {0, -1, 1}, {-1, 0, 3},
        {1, 0, 5}, {0, 1, 7}, {-1, -1, 0},
        {1, -1, 2}, {-1, 1, 6}, {1, 1, 8}};
    public static final int[][] TILES_GRID_25 = {{0, 0, 12}, {0, -1, 7}, {-1, 0, 11},
        {1, 0, 13}, {0, 1, 17}, {-1, -1, 6},
        {1, -1, 8}, {-1, 1, 16}, {1, 1, 18},
        {-1, -2, 1}, {0, -2, 2}, {1, -2, 3},
        {-2, -1, 5}, {-2, 0, 10}, {-2, 1, 15},
        {2, -1, 9}, {2, 0, 14}, {2, 1, 19},
        {-1, 2, 21}, {0, 2, 22}, {1, 2, 23},
        {-2, -2, 0}, {2, -2, 4}, {-2, 2, 20},
        {2, 2, 24}};
    private Tile[] mapTiles;
    private Bitmap big_image;
    private Canvas mapCanvas;
    private Paint paint;
    private Object[] locker;
    private MemoryTilesCache memoryCache;
    private int tilesLineSize;
    private int tileCacheSize;

    public TilesCache() {
        tileCacheSize = ConfigurationManager.getInstance().getInt(ConfigurationManager.SCREEN_SIZE);

        if (tileCacheSize == TILES_CACHE_SMALL) {
            tilesLineSize = 3;
        } else if (tileCacheSize == TILES_CACHE_LARGE) {
            tilesLineSize = 5;
        }

        mapTiles = new Tile[tileCacheSize];
        locker = new Object[tileCacheSize];

        for (int i = 0; i < tileCacheSize; i++) {
            locker[i] = new Object();
        }

        initialize();

        int bitmapSize = tilesLineSize * ConfigurationManager.TILE_SIZE;

        big_image = Bitmap.createBitmap(bitmapSize, bitmapSize, ConfigurationManager.getInstance().getBitmapConfig());

        mapCanvas = new Canvas(big_image);
        paint = new Paint();
    }

    public void setTile(int index, Tile image) {
        //System.out.println("TilesCache.setTile " + index + " " + image.getXTile() + " " + image.getYTile() + " " + image.getImage().toString());
        if (image.isPersistent()) {
            memoryCache.addTile(image);
        }
        synchronized (locker[index]) {
            mapTiles[index] = image;
        }
    }

    public Bitmap getImage() {
        drawImage();
        return big_image;
    }

    private void drawImage() {
        for (int i = 0; i < tileCacheSize; i++) {
            mapCanvas.drawBitmap(getTile(i).getImage(), (i % tilesLineSize) * ConfigurationManager.TILE_SIZE, (i / tilesLineSize) * ConfigurationManager.TILE_SIZE, paint);
        }

        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_GRID)) {
            for (int i = 1; i < tilesLineSize; i++) {
                mapCanvas.drawLine(0, i * ConfigurationManager.TILE_SIZE, tilesLineSize * ConfigurationManager.TILE_SIZE, i * ConfigurationManager.TILE_SIZE, paint);
                mapCanvas.drawLine(i * ConfigurationManager.TILE_SIZE, 0, i * ConfigurationManager.TILE_SIZE, tilesLineSize * ConfigurationManager.TILE_SIZE, paint);
            }
        }
   }

   public Tile getTile(int index) {
        //System.out.println("Reading tile: " + index);
        if (index >= 0 && index < tileCacheSize) {
            synchronized (locker[index]) {
                return mapTiles[index];
            }
        } else {
            return null;
        }
    }

    public final void initialize() {
        memoryCache = new MemoryTilesCache(this);
        for (int i = 0; i < tileCacheSize; i++) {
            setTile(i, TileFactory.getTileMissing());
        }
    }

    public void cacheTile(Tile tile, boolean recycle) {
        try {
            // LoggerUtils.debug("Caching tile: " + tile.getXTile() + " " + tile.getYTile() + " " + tile.getZoom() + " " + tile.getLatitude() + " " + tile.getLongtude() + " " + tile.getPersist());
            if (tile.isPersistent() && !recycle) {
                memoryCache.addTile(tile);
            }

            if (tile.getXTile() != -1 && tile.getYTile() != -1 && tile.getZoom() != -1 && tile.isPersistent()) {
                //LoggerUtils.debug("Caching tile: " + tile.getXTile() + " " + tile.getYTile() + " " + tile.getZoom());
                String label = tile.getXTile() + PersistenceManager.SEPARATOR_CHAR + tile.getYTile() + PersistenceManager.SEPARATOR_CHAR + tile.getZoom() + "tile" + PersistenceManager.FORMAT_PNG;
                cacheTile(tile, label, recycle);
            } else if (tile.getLatitude() != -999.0 && tile.getLongtude() != -999.0 && tile.isPersistent()) {
                double[] coords = MercatorUtils.normalizeE6(new double[]{tile.getLatitude(), tile.getLongtude()});
                //LoggerUtils.debug("Caching tile: " + coords[0] + " " + coords[1]);
                String label = coords[0] + PersistenceManager.SEPARATOR_CHAR + coords[1] + "tile" + PersistenceManager.FORMAT_PNG;
                cacheTile(tile, label, recycle);
            }
        } catch (Exception e) {
            LoggerUtils.error("TilesCache.cacheTile error: ", e);
        }
    }

    private void cacheTile(Tile tile, String label, boolean recycle) {
        try {
            if (!PersistenceManagerFactory.getPersistenceManagerInstance().tileExists(label)) {
                PersistenceManagerFactory.getPersistenceManagerInstance().saveImageFile(tile.getImage(), label);
                if (recycle) {
                    tile.recycle();
                }
            }
        } catch (Exception e) {
            LoggerUtils.error("TilesCache.cacheTile error: ", e);
        }
    }

    public Tile getTileFromCache(int zoom, int x, int y) {
        Tile tile = null;

        try {
            int pos = memoryCache.containsTile(x, y, zoom);
            if (pos != -1) {
                tile = memoryCache.getTile(pos);
            } else {
                //LoggerUtils.debug("Searching for tile: " + x + " " + y + " " + zoom);
                String label = x + PersistenceManager.SEPARATOR_CHAR + y + PersistenceManager.SEPARATOR_CHAR + zoom + "tile" + PersistenceManager.FORMAT_PNG;
                if (PersistenceManagerFactory.getPersistenceManagerInstance().tileExists(label)) {
                    Bitmap image = PersistenceManagerFactory.getPersistenceManagerInstance().readImageFile(label);
                    if (image != null) {
                        tile = TileFactory.getTile(x, y, zoom, image);
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            System.gc();
            LoggerUtils.error("TilesCache.getTileFromCache error: ", e);
        } catch (Exception e) {
            LoggerUtils.error("TilesCache.getTileFromCache exception: ", e);
        }

        return tile;
    }

    public boolean containsTile(Tile tile) {
        for (int i = 0; i < mapTiles.length; i++) {
            if (tile.equals(mapTiles[i])) {
                return true;
            }
        }
        return false;
    }

    public void clearAll() {
       memoryCache.clearAll();
       for (int i=0;i<mapTiles.length;i++) {
            Tile t = mapTiles[i];
            t.recycle();
            t = null;
       }
       mapTiles = null;
       big_image.recycle();
       big_image = null;
    }
}
