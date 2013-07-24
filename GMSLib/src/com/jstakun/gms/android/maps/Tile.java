/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.maps;

import android.graphics.Bitmap;

/**
 *
 * @author jstakun
 */
public class Tile {

    private Bitmap tile;
    private double latitude;
    private double longitude;
    private int xtile;
    private int ytile;
    private int zoom;
    private boolean persistent;

    public Tile(Bitmap image, double lat, double lon, int x, int y, int z, boolean p) {
        tile = image;
        latitude = lat;
        longitude = lon;
        xtile = x;
        ytile = y;
        zoom = z;
        persistent = p;
    }

    public Bitmap getImage() {
        return tile;
    }

    //public byte[] getImageData() {
    //    return imageData;
    //}
    public double getLatitude() {
        return latitude;
    }

    public double getLongtude() {
        return longitude;
    }

    public int getXTile() {
        return xtile;
    }

    public int getYTile() {
        return ytile;
    }

    public int getZoom() {
        return zoom;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void recycle() {
        if (isPersistent()) {
            synchronized (this) {
                if (tile != null && !tile.isRecycled()) {
                    //System.out.println("Recycling bitmap: " + tile.toString());
                    tile.recycle();
                    tile = null;
                }
            }
        }
    }
}
