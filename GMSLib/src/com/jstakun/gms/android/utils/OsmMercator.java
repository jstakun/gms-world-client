/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.utils;

// License: GPL. Copyright 2007 by Tim Haussmann

import net.dclausen.microfloat.MicroDouble;


/**
 * This class implements the Mercator Projection as it is used by Openstreetmap
 * (and google). It provides methods to translate coordinates from 'map space'
 * into latitude and longitude (on the WGS84 ellipsoid) and vice versa. Map
 * space is measured in pixels. The origin of the map space is the top left
 * corner. The map space origin (0,0) has latitude ~85 and longitude -180
 *
 * @author Tim Haussmann
 *
 */

public class OsmMercator {

    private static int TILE_SIZE = 256;
    public static final double MAX_LAT = 85.05112877980659;
    public static final double MIN_LAT = -85.05112877980659;

    public static double radius(int aZoomlevel) {
        return (TILE_SIZE * (1 << aZoomlevel)) / (2.0 * Math.PI);
    }

    /**
     * Returns the absolut number of pixels in y or x, defined as: 2^Zoomlevel *
     * TILE_WIDTH where TILE_WIDTH is the width of a tile in pixels
     *
     * @param aZoomlevel
     * @return
     */
    public static int getMaxPixels(int aZoomlevel) {
        return TILE_SIZE * (1 << aZoomlevel);
    }

    public static int falseEasting(int aZoomlevel) {
        return getMaxPixels(aZoomlevel) / 2;
    }

    public static int falseNorthing(int aZoomlevel) {
        return (-1 * getMaxPixels(aZoomlevel) / 2);
    }

    /**
     * Transform longitude to pixelspace
     *
     * <p>
     * Mathematical optimization<br>
     * <code>
     * x = radius(aZoomlevel) * toRadians(aLongitude) + falseEasting(aZoomLevel)<br>
     * x = getMaxPixels(aZoomlevel) / (2 * PI) * (aLongitude * PI) / 180 + getMaxPixels(aZoomlevel) / 2<br>
     * x = getMaxPixels(aZoomlevel) * aLongitude / 360 + 180 * getMaxPixels(aZoomlevel) / 360<br>
     * x = getMaxPixels(aZoomlevel) * (aLongitude + 180) / 360<br>
     * </code>
     * </p>
     *
     * @param aLongitude
     *            [-180..180]
     * @return [0..2^Zoomlevel*TILE_SIZE[
     * @author Jan Peter Stotz
     */
    public static int LonToX(double aLongitude, final int aZoomlevel) {
        int mp = getMaxPixels(aZoomlevel);
        int x = (int) ((mp * (aLongitude + 180l)) / 360l);
        x = Math.min(x, mp - 1);
        return x;
    }

    /**
     * Transforms latitude to pixelspace
     * <p>
     * Mathematical optimization<br>
     * <code>
     * log(u) := log((1.0 + sin(toRadians(aLat))) / (1.0 - sin(toRadians(aLat))<br>
     *
     * y = -1 * (radius(aZoomlevel) / 2 * log(u)))) - falseNorthing(aZoomlevel))<br>
     * y = -1 * (getMaxPixel(aZoomlevel) / 2 * PI / 2 * log(u)) - -1 * getMaxPixel(aZoomLevel) / 2<br>
     * y = getMaxPixel(aZoomlevel) / (-4 * PI) * log(u)) + getMaxPixel(aZoomLevel) / 2<br>
     * y = getMaxPixel(aZoomlevel) * ((log(u) / (-4 * PI)) + 1/2)<br>
     * </code>
     * </p>
     * @param aLat
     *            [-90...90]
     * @return [0..2^Zoomlevel*TILE_SIZE[
     * @author Jan Peter Stotz
     */
    public static int LatToY(final double aLat, final int aZoomlevel) {
        double aLat1 = aLat;
        if (aLat1 < MIN_LAT)
            aLat1 = MIN_LAT;
        else if (aLat1 > MAX_LAT)
            aLat1 = MAX_LAT;
        double sinLat = Math.sin(Math.toRadians(aLat1));
        double log = Double.longBitsToDouble(MicroDouble.log(Double.doubleToLongBits((1.0 + sinLat) / (1.0 - sinLat))));
        int mp = getMaxPixels(aZoomlevel);
        int y = (int) (mp * (0.5 - (log / (4.0 * Math.PI))));
        y = Math.min(y, mp - 1);
        return y;
    }

    /**
     * Transforms pixel coordinate X to longitude
     *
     * <p>
     * Mathematical optimization<br>
     * <code>
     * lon = toDegree((aX - falseEasting(aZoomlevel)) / radius(aZoomlevel))<br>
     * lon = 180 / PI * ((aX - getMaxPixels(aZoomlevel) / 2) / getMaxPixels(aZoomlevel) / (2 * PI)<br>
     * lon = 180 * ((aX - getMaxPixels(aZoomlevel) / 2) / getMaxPixels(aZoomlevel))<br>
     * lon = 360 / getMaxPixels(aZoomlevel) * (aX - getMaxPixels(aZoomlevel) / 2)<br>
     * lon = 360 * aX / getMaxPixels(aZoomlevel) - 180<br>
     * </code>
     * </p>
     * @param aX
     *            [0..2^Zoomlevel*TILE_WIDTH[
     * @return ]-180..180[
     * @author Jan Peter Stotz
     */
    public static double XToLon(final int aX, final int aZoomlevel) {
        return ((360d * aX) / getMaxPixels(aZoomlevel)) - 180.0;
    }

    /**
     * Transforms pixel coordinate Y to latitude
     *
     * @param aY
     *            [0..2^Zoomlevel*TILE_WIDTH[
     * @return [MIN_LAT..MAX_LAT] is about [-85..85]
     */
    public static double YToLat(int aY, final int aZoomlevel) {
        aY += falseNorthing(aZoomlevel);
        double latitude = (Math.PI / 2) - (2 * Double.longBitsToDouble(MicroDouble.atan(MicroDouble.exp(Double.doubleToLongBits(-1.0 * aY / radius(aZoomlevel))))));
        return -1 * Math.toDegrees(latitude);
    }

    public static double[] adjust(final double lat, final double lng, final int deltaX, final int deltaY, final int z) {
        return new double[]{XToLon(LonToX(lng,z) + deltaX,z), YToLat((LatToY(lat,z) + deltaY),z)};
    }

}
