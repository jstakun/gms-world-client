/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.maps;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 *
 * @author jstakun
 */
public class MemoryTilesCache {

    private static int SIZE;
    private Tile[] cache;
    private int currentPos = 0;
    private Object[] locker;
    private TilesCache parent;

    protected MemoryTilesCache(TilesCache parent) {
        SIZE = ConfigurationManager.getInstance().getInt(ConfigurationManager.MEMORY_TILES_CACHE_SIZE);
        currentPos = 0;
        cache = new Tile[SIZE];
        locker = new Object[SIZE];
        for (int i = 0; i < SIZE; i++) {
            locker[i] = new Object();
        }
        this.parent = parent;
    }

    protected void addTile(Tile tile) {
        if (currentPos == SIZE) {
            currentPos = 0;
        }

        if (cache[currentPos] != null) {
            boolean containsTile = true;
            while (containsTile) {
                if (parent.containsTile(cache[currentPos])) {
                    currentPos++;
                } else {
                    containsTile = false;
                }
            }
        }

        synchronized (locker[currentPos]) {
            if (containsTile(tile.getXTile(), tile.getYTile(), tile.getZoom()) == -1) {
                //System.out.println("Adding tile to memory cache: " + currentPos);

                if (cache[currentPos] != null) {
                    cache[currentPos].recycle();
                    cache[currentPos] = null;
                }

                cache[currentPos] = tile;

                currentPos++;
            }
        }
    }

    protected int containsTile(int x, int y, int z) {
        for (int i = 0; i < SIZE; i++) {
            if (cache[i] != null) {
                if (cache[i].getXTile() == x && cache[i].getYTile() == y && cache[i].getZoom() == z) {
                    return i;
                }
            }
        }

        return -1;
    }

    protected Tile getTile(int index) {
        Tile result = null;

        if (index >= 0 && index < SIZE) {
            synchronized (locker[index]) {
                if (cache[index] != null) {
                    //System.out.println("Reading tile from memory cache: " + index);
                    result = cache[index];
                }
            }
        }

        return result;
    }

    protected boolean availableMemory() {
        double memory = (double) Runtime.getRuntime().freeMemory() / (double) Runtime.getRuntime().totalMemory();
        int percentage = (int) (memory * 100);
        LoggerUtils.debug("Free memory: " + percentage + "%");
        return (memory >= 0.3);
    }

    protected void clearAll() {
        for (int i=0;i<cache.length;i++) {
            Tile t = cache[i];
            if (t != null) {
              t.recycle();
            }
        }
        currentPos = 0;
    }
}
