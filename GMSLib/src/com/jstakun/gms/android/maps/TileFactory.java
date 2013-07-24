/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.maps;

import android.graphics.Bitmap;
import com.jstakun.gms.android.data.IconCache;

/**
 *
 * @author jstakun
 */
public class TileFactory {

    private static Tile missing = null;
    private static Tile loading = null;

    public static Tile getTileMissing() {
        if (missing == null) {
            missing = new Tile(IconCache.getInstance().getImageResource(IconCache.IMAGE_MISSING), -999.0, -999.0, -1, -1, -1, false);
        }
        return missing;
    }

    public static Tile getTileLoading() {
        if (loading == null) {
            loading = new Tile(IconCache.getInstance().getImageResource(IconCache.IMAGE_LOADING_TILE), -999.0, -999.0, -1, -1, -1, false);
        }
        return loading;
    }

    public static Tile getTile(int x, int y, int zoom, Bitmap image) throws Exception {
        if (image == null) {
           throw new Exception("TileFactory exception: wrong parameter image=null");
        } else {
            return new Tile(image, -999.0, -999.0, x, y, zoom, true);
        }
    }
}
